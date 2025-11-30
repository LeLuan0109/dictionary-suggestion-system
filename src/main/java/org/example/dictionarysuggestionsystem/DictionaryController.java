package org.example.dictionarysuggestionsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import org.example.dictionarysuggestionsystem.model.DictionaryEntry;
import org.example.dictionarysuggestionsystem.service.DictionaryService;
import org.example.dictionarysuggestionsystem.utils.NormalizerUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DictionaryController {
    @FXML private TextField inputField;
    @FXML private Button searchButton;
    @FXML private ListView<String> suggestionList;
    @FXML private Label runtimeTrieLabel;
    @FXML private Label runtimeLevenshteinLabel;
    @FXML private Label runtimeTfidfLabel;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private TableView<DictionaryEntry> tableView;
    @FXML private TableColumn<DictionaryEntry, String> colWord;
    @FXML private TableColumn<DictionaryEntry, String> colMeaning;
    @FXML private TableColumn<DictionaryEntry, Number> colFrequency;
    @FXML private TableColumn<DictionaryEntry, String> colTags;
    @FXML private StackPane notificationContainer;

    private final ObservableList<String> suggestions = FXCollections.observableArrayList();
    private DictionaryService service;

    @FXML
    public void initialize() {
        suggestionList.setItems(suggestions);
        service = new DictionaryService(new File("dictionary.json"));
        inputField.textProperty().addListener((obs, o, n) -> onSearch());
        // Không auto hiển thị toàn bộ khi khởi động
        suggestions.clear();
        if (runtimeTrieLabel != null) runtimeTrieLabel.setText("Trie: -");
        if (runtimeLevenshteinLabel != null) runtimeLevenshteinLabel.setText("Lev: -");
        if (runtimeTfidfLabel != null) runtimeTfidfLabel.setText("TF-IDF: -");
        setupTable();
        refreshTable();
        
        // Thêm event handler cho click chuột
        suggestionList.setOnMouseClicked(this::onSuggestionClick);
        
        // Thêm event handler cho phím Enter
        suggestionList.setOnKeyPressed(this::onSuggestionKeyPress);
    }

    @FXML
    private void onSearch() {
        String q = inputField.getText() == null ? "" : inputField.getText().trim();
        if (q.isEmpty()) {
            suggestions.clear();
            runtimeTrieLabel.setText("Trie: -");
            runtimeLevenshteinLabel.setText("Lev: -");
            runtimeTfidfLabel.setText("TF-IDF: -");
            return;
        }
        List<String> merged = new ArrayList<>();
        DictionaryService.TimedResult<List<String>> trieRes = service.suggestByPrefix(q, 10, true);
        runtimeTrieLabel.setText("Trie: " + trieRes.micros + " µs");
        merged.addAll(trieRes.value);

        // Chỉ thêm Levenshtein nếu input >= 2 ký tự và kết quả prefix còn ít (<5)
        if (q.length() >= 2 && merged.size() < 5) {
            String nq = NormalizerUtil.normalize(q);
            int dynamicMax = nq.length() <= 3 ? 1 : 2;
            DictionaryService.TimedResult<List<String>> levRes = service.suggestByLevenshteinFiltered(q, 10, dynamicMax);
            runtimeLevenshteinLabel.setText("Lev: " + levRes.micros + " µs");
            // Lọc thêm: ứng viên phải bắt đầu bằng 2 ký tự đầu (sau chuẩn hóa)
            String firstTwo = nq.length() >= 2 ? nq.substring(0, 2) : nq;
            for (String s : levRes.value) {
                String ns = NormalizerUtil.normalize(s);
                if (ns.startsWith(firstTwo) && !merged.contains(s)) merged.add(s);
            }
        } else {
            runtimeLevenshteinLabel.setText("Lev: skipped");
        }

        // Chỉ dùng TF-IDF nếu kết quả còn ít (<5)
        // Chỉ bật TF-IDF với truy vấn dài (>=4) và còn ít kết quả
        if (q.length() >= 4 && merged.size() < 5) {
            DictionaryService.TimedResult<List<DictionaryEntry>> tfidfRes = service.suggestByTfidf(q, 5);
            runtimeTfidfLabel.setText("TF-IDF: " + tfidfRes.micros + " µs");
            List<String> tfidfWords = tfidfRes.value.stream().map(DictionaryEntry::getWord).collect(Collectors.toList());
            for (String s : tfidfWords) if (!merged.contains(s)) merged.add(s);
        } else {
            runtimeTfidfLabel.setText("TF-IDF: skipped");
        }

        // Giới hạn tổng số gợi ý
        if (merged.size() > 10) merged = new ArrayList<>(merged.subList(0, 10));
        suggestions.setAll(merged);
    }

    @FXML
    private void onAdd() {
        EntryForm form = new EntryForm(null, null, 0, "");
        Optional<EntryForm> res = form.showAndWait();
        res.ifPresent(f -> {
            try {
                service.add(new DictionaryEntry(f.word, f.meaning, f.frequency, f.tagsList()));
                onSearch();
            } catch (IOException e) {
                showError("Không thể thêm từ: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onEdit() {
        String selected = suggestionList.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Chọn một từ để sửa."); return; }
        DictionaryEntry existing = service.getAllEntries().stream()
            .filter(e -> e.getWord().equalsIgnoreCase(selected))
            .findFirst().orElse(null);
        if (existing == null) { showInfo("Không tìm thấy mục đã chọn."); return; }
        EntryForm form = new EntryForm(existing.getWord(), existing.getMeaning(), existing.getFrequency(),
            existing.getTags() == null ? "" : String.join(",", existing.getTags()));
        Optional<EntryForm> res = form.showAndWait();
        res.ifPresent(f -> {
            try {
                service.edit(new DictionaryEntry(f.word, f.meaning, f.frequency, f.tagsList()));
                onSearch();
            } catch (IOException e) {
                showError("Không thể sửa từ: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onDelete() {
        String selected = suggestionList.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Chọn một từ để xóa."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Xóa từ '" + selected + "'?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.delete(selected);
                    onSearch();
                } catch (IOException e) {
                    showError("Không thể xóa từ: " + e.getMessage());
                }
            }
        });
    }

    private void showInfo(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }

    @FXML
    private void onReload() {
        try {
            service.reload();
            onSearch();
            refreshTable();
        } catch (IOException e) {
            showError("Không thể reload: " + e.getMessage());
        }
    }

    @FXML
    private void onExit() {
        System.exit(0);
    }

    @FXML
    private void onAbout() {
        showInfo("Dictionary Suggestion System\nJavaFX + Trie + Levenshtein + TF-IDF");
    }

    @FXML
    private void onEditSelected() { // for Manage tab
        DictionaryEntry selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Chọn một dòng để sửa."); return; }
        EntryForm form = new EntryForm(selected.getWord(), selected.getMeaning(), selected.getFrequency(),
            selected.getTags() == null ? "" : String.join(",", selected.getTags()));
        form.showAndWait().ifPresent(f -> {
            try {
                service.edit(new DictionaryEntry(f.word, f.meaning, f.frequency, f.tagsList()));
                onSearch();
                refreshTable();
            } catch (IOException e) {
                showError("Không thể sửa: " + e.getMessage());
            }
        });
    }

    @FXML
    private void onDeleteSelected() { // for Manage tab
        DictionaryEntry selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) { showInfo("Chọn một dòng để xóa."); return; }
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION, "Xóa từ '" + selected.getWord() + "'?", ButtonType.YES, ButtonType.NO);
        conf.setHeaderText(null);
        conf.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.delete(selected.getWord());
                    onSearch();
                    refreshTable();
                } catch (IOException e) {
                    showError("Không thể xóa: " + e.getMessage());
                }
            }
        });
    }

    private void setupTable() {
        if (colWord != null) colWord.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getWord()));
        if (colMeaning != null) colMeaning.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getMeaning()));
        if (colFrequency != null) colFrequency.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().getFrequency()));
        if (colTags != null) colTags.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTags() == null ? "" : String.join(", ", cd.getValue().getTags())));
    }

    private void refreshTable() {
        if (tableView != null) {
            tableView.getItems().setAll(service.getAllEntries());
        }
    }

    private void onSuggestionClick(MouseEvent event) {
        if (event.getClickCount() == 1) {
            String selected = suggestionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                incrementFrequencyForWord(selected);
                showMeaningNotification(selected);
            }
        }
    }

    private void onSuggestionKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            String selected = suggestionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                incrementFrequencyForWord(selected);
                showMeaningNotification(selected);
            }
        }
    }

    private void incrementFrequencyForWord(String word) {
        try {
            service.incrementFrequency(word);
            refreshTable();
        } catch (IOException e) {
            showError("Không thể cập nhật frequency: " + e.getMessage());
        }
    }

    private void showMeaningNotification(String word) {
        // Tìm meaning của từ
        DictionaryEntry entry = service.getAllEntries().stream()
            .filter(e -> e.getWord().equalsIgnoreCase(word))
            .findFirst()
            .orElse(null);
        
        if (entry == null || entry.getMeaning() == null) {
            return;
        }

        // Lấy container - ưu tiên notificationContainer từ FXML
        StackPane container = notificationContainer;
        
        // Nếu không có, tìm từ root scene
        if (container == null && suggestionList != null && suggestionList.getScene() != null) {
            javafx.scene.Node root = suggestionList.getScene().getRoot();
            if (root instanceof StackPane) {
                StackPane rootPane = (StackPane) root;
                // Tìm notificationContainer trong root StackPane
                for (javafx.scene.Node node : rootPane.getChildren()) {
                    if (node instanceof StackPane) {
                        StackPane sp = (StackPane) node;
                        // Kiểm tra nếu là notificationContainer
                        if (sp.getAlignment() == Pos.BOTTOM_RIGHT && sp.isMouseTransparent()) {
                            container = sp;
                            break;
                        }
                    }
                }
                // Nếu không tìm thấy, tạo container mới và thêm vào root
                if (container == null) {
                    container = new StackPane();
                    container.setAlignment(Pos.BOTTOM_RIGHT);
                    container.setMouseTransparent(true);
                    container.setPadding(new Insets(20));
                    rootPane.getChildren().add(container);
                }
            }
        }

        if (container == null) {
            return;
        }

        // Xóa notification cũ nếu có
        container.getChildren().removeIf(node -> node instanceof VBox && node.getStyle().contains("rgba(0, 0, 0, 0.85)"));

        // Tạo VBox chứa notification
        VBox notificationBox = new VBox(8);
        notificationBox.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.85); " +
            "-fx-background-radius: 10; " +
            "-fx-padding: 15; " +
            "-fx-max-width: 400; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);"
        );

        // Label cho từ
        Label wordLabel = new Label(word);
        wordLabel.setStyle(
            "-fx-font-size: 18px; " +
            "-fx-font-weight: bold; " +
            "-fx-text-fill: #4CAF50;"
        );

        // Label cho meaning
        Label meaningLabel = new Label(entry.getMeaning());
        meaningLabel.setStyle(
            "-fx-font-size: 14px; " +
            "-fx-text-fill: white; " +
            "-fx-wrap-text: true;"
        );
        meaningLabel.setMaxWidth(380);


        notificationBox.getChildren().addAll(wordLabel, meaningLabel);

        // Thêm vào container
        container.getChildren().add(notificationBox);

        // Tạo biến final để sử dụng trong lambda
        final StackPane finalContainer = container;
        final VBox finalNotificationBox = notificationBox;

        // Animation fade in
        notificationBox.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), notificationBox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Tự động ẩn sau 2 giây với fade out
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(2), e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), finalNotificationBox);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(event -> {
                    finalContainer.getChildren().remove(finalNotificationBox);
                });
                fadeOut.play();
            })
        );
        timeline.play();
    }

    private static class EntryForm extends Dialog<EntryForm> {
        final TextField wordField = new TextField();
        final TextArea meaningArea = new TextArea();
        final TextField freqField = new TextField();
        final TextField tagsField = new TextField();
        String word; String meaning; long frequency; String tags;
        EntryForm(String w, String m, long f, String t) {
            setTitle("Từ điển - Thêm/Sửa");
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            wordField.setPromptText("word");
            meaningArea.setPromptText("meaning");
            freqField.setPromptText("frequency");
            tagsField.setPromptText("tags, phân cách bằng dấu phẩy");
            wordField.setText(w == null ? "" : w);
            meaningArea.setText(m == null ? "" : m);
            freqField.setText(String.valueOf(f));
            tagsField.setText(t == null ? "" : t);
            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10);
            grid.addRow(0, new Label("Word"), wordField);
            grid.addRow(1, new Label("Meaning"), meaningArea);
            grid.addRow(2, new Label("Frequency"), freqField);
            grid.addRow(3, new Label("Tags"), tagsField);
            getDialogPane().setContent(grid);
            setResultConverter(bt -> {
                if (bt == ButtonType.OK) {
                    this.word = wordField.getText().trim();
                    this.meaning = meaningArea.getText().trim();
                    try { this.frequency = Long.parseLong(freqField.getText().trim()); } catch (Exception ex) { this.frequency = 0; }
                    this.tags = tagsField.getText().trim();
                    return this;
                }
                return null;
            });
        }
        List<String> tagsList() {
            if (tags.isEmpty()) return List.of();
            String[] parts = tags.split(",");
            List<String> out = new ArrayList<>();
            for (String p : parts) { String s = p.trim(); if (!s.isEmpty()) out.add(s); }
            return out;
        }
    }
}



