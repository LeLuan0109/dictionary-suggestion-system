Dictionary Suggestion System (JavaFX + Maven)

Ứng dụng JavaFX gợi ý từ điển theo thời gian thực, hỗ trợ:
- Trie (prefix) + BFS/DFS
- Levenshtein (gợi ý gần đúng)
- TF‑IDF (gợi ý liên quan theo ngữ nghĩa)
- Chuẩn hóa bỏ dấu tiếng Việt khi tìm kiếm
- CRUD (thêm/sửa/xóa) từ điển, lưu JSON bằng Jackson
- Hiển thị thời gian chạy từng thuật toán, logging bằng SLF4J

Yêu cầu
- JDK 17
- Maven 3.9+
- IntelliJ IDEA (khuyến nghị) hoặc dòng lệnh Maven

Chạy nhanh
- IntelliJ:
    1. Mở dự án -> Maven load dependencies.
    2. VM options: --enable-native-access=ALL-UNNAMED --enable-native-access=javafx.graphics
    3. Chạy class org.example.dictionarysuggestionsystem.DictionaryApp
- Maven CLI: mvn clean javafx:run

Kiến trúc & Module
- DictionaryApp: khởi động JavaFX, nạp dictionary.fxml
- DictionaryController: điều khiển UI, kết nối service, hiển thị gợi ý và CRUD
- model/DictionaryEntry: word, meaning, frequency, tags
- trie/Trie: insert, search, prefixSuggest (BFS/DFS)
- algorithms/Levenshtein: khoảng cách chỉnh sửa
- algorithms/TfidfRanker: xếp hạng theo TF‑IDF (lọc điểm 0)
- service/DictionaryService: kết hợp Trie + Lev + TF‑IDF, đo thời gian, thao tác dữ liệu
- repository/JsonDictionaryRepository: lưu/đọc JSON bằng Jackson
- util/NormalizerUtil: bỏ dấu + lowercase để tìm kiếm không phân biệt dấu

Cơ chế gợi ý
- Chuẩn hóa chuỗi (bỏ dấu, lowercase) trước khi so khớp.
- Prefix (Trie) luôn chạy trước; nếu input chỉ 1 ký tự -> chỉ dùng prefix.
- Levenshtein: chỉ chạy nếu input ≥ 2 và kết quả prefix còn ít (< 5).
    - Ngưỡng động: độ dài ≤ 3 -> maxDistance = 1; ngược lại = 2
    - Lọc bổ sung: từ ứng viên (đã chuẩn hóa) phải bắt đầu cùng 2 ký tự đầu của truy vấn
- TF‑IDF: chỉ chạy khi input ≥ 4 và tổng gợi ý vẫn ít (< 5); bỏ các kết quả có điểm 0
- Tối đa 10 gợi ý, ưu tiên: prefix -> gần đúng -> TF‑IDF

Dữ liệu
- File runtime: dictionary.json (tạo ở thư mục làm việc khi chạy).
- Seed: src/main/resources/dictionary-seed.json. Lần đầu (hoặc khi trống) sẽ tự nạp seed và ghi ra dictionary.json.
- CRUD trong tab “Quản lý” sẽ cập nhật dictionary.json và rebuild Trie.

Khuyến nghị .gitignore
```
/target/
/dictionary.json
```

Test
- mvn test (đã có test cho Trie và Levenshtein)

Ghi log
- SLF4J SimpleLogger cấu hình tại src/main/resources/simplelogger.properties

Lỗi thường gặp
- Native access warning: thêm VM options như trên.
- Không thấy dữ liệu: kiểm tra dictionary.json ở thư mục làm việc; xóa file để seed lại nếu cần.
