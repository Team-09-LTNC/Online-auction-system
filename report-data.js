window.REPORT_DATA = {
  "bai-1": {
    title: "Bài 1: Thao tác cơ bản với tập tin và thư mục",
    summary:
      "Thực hành trọn vẹn quy trình tạo, đổi tên, sao chép, di chuyển, xóa và khôi phục dữ liệu trong File Explorer. Đây là bài duy nhất sử dụng chuỗi ảnh chụp từng bước để chứng minh thao tác.",
    tags: ["File Explorer", "13 ảnh gốc", "Quản lý dữ liệu"],
    table: {
      targetHeading: "So sánh Delete và Shift + Delete",
      caption: "Bảng so sánh hai phương thức xóa dữ liệu trên Windows",
      columns: ["Tiêu chí", "Delete", "Shift + Delete"],
      rows: [
        ["Cách xử lý", "Chuyển tệp vào Recycle Bin", "Xóa trực tiếp khỏi vị trí lưu trữ"],
        ["Khả năng khôi phục", "Có thể Restore nếu chưa dọn Recycle Bin", "Không thể khôi phục bằng thao tác thông thường"],
        ["Trường hợp sử dụng", "Xóa tạm thời hoặc khi chưa chắc chắn", "Chỉ dùng khi chắc chắn không còn cần dữ liệu"],
        ["Mức độ rủi ro", "Thấp hơn", "Cao hơn"],
      ],
    },
  },
  "bai-2": {
    title: "Bài 2: Tìm kiếm và tổng hợp tài liệu khoa học bằng AI",
    summary:
      "Sử dụng Elicit để xây dựng tổng quan tài liệu về vật liệu Graphene trong pin Lithium-Sulfur, so sánh cơ chế tác động, hiệu suất và hạn chế của các hướng nghiên cứu tiêu biểu.",
    tags: ["Elicit", "Graphene", "Pin Li-S"],
    metrics: [
      ["Công cụ", "Elicit"],
      ["Đối tượng", "GO, rGO và Graphene 3D"],
      ["Đầu ra", "Bảng so sánh tài liệu"],
    ],
    content: [
      "Giới thiệu bài tập",
      "Bài tập tập trung vào việc sử dụng công cụ AI hỗ trợ nghiên cứu để tìm kiếm, trích xuất, so sánh và tổng hợp tài liệu khoa học về vật liệu Graphene trong pin Lithium-Sulfur. Thay vì chỉ thu thập danh sách bài báo, quá trình thực hiện hướng đến việc trả lời một câu hỏi nghiên cứu cụ thể và chỉ ra cả giá trị lẫn hạn chế của từng hướng tiếp cận.",
      "Thông qua bài tập, em rèn luyện kỹ năng xác định câu hỏi nghiên cứu, tìm tài liệu có định hướng, đọc bảng trích xuất, so sánh kết quả giữa nhiều công trình và tự tổng hợp nhận định thay vì sao chép kết luận do AI tạo ra.",
      "Thông tin bài tập",
      "• Chủ đề: Ứng dụng vật liệu nanocomposite dựa trên Graphene nhằm cải thiện hiệu suất điện hóa của pin Lithium-Sulfur.",
      "• Câu hỏi nghiên cứu: GO và rGO cải thiện dung lượng lưu trữ, độ dẫn điện và hạn chế hiệu ứng con thoi của polysulfide như thế nào?",
      "• Công cụ sử dụng: Elicit.",
      "• Nội dung chính: Tìm kiếm tài liệu, trích xuất dữ liệu, so sánh cơ chế tác động, hiệu suất, hạn chế và tổng hợp xu hướng nghiên cứu.",
      "Mục tiêu và kỹ năng đạt được",
      "• Xác định câu hỏi nghiên cứu: Biết thu hẹp một chủ đề rộng thành vấn đề có thể tìm kiếm và so sánh.",
      "• Tìm kiếm có định hướng: Biết sử dụng thuật ngữ chuyên môn như Graphene oxide, Reduced Graphene oxide, cathode và shuttle effect.",
      "• Trích xuất dữ liệu: Biết đọc và tổ chức thông tin theo tác giả, phương pháp, kết quả, thông số và hạn chế.",
      "• Đánh giá tài liệu: Biết nhận diện ưu điểm của kết quả phòng thí nghiệm và các rào cản khi triển khai thực tế.",
      "• Tổng hợp học thuật: Biết rút ra xu hướng chung từ nhiều nghiên cứu thay vì trình bày từng nguồn riêng lẻ.",
      "I. Xác định chủ đề và câu hỏi nghiên cứu",
      "Việc xác định câu hỏi nghiên cứu giúp quá trình tìm kiếm trên Elicit có trọng tâm và tạo ra các tiêu chí so sánh thống nhất.",
      "1. Lựa chọn hệ pin Lithium-Sulfur vì đây là công nghệ có mật độ năng lượng lý thuyết cao nhưng còn gặp hạn chế về độ dẫn điện của sulfur và hiện tượng polysulfide hòa tan.",
      "2. Tập trung vào Graphene, Graphene oxide và Reduced Graphene oxide vì các vật liệu này có thể tạo mạng dẫn điện, giữ vật liệu hoạt tính và hạn chế sự khuếch tán polysulfide.",
      "3. Xây dựng câu hỏi nghiên cứu xoay quanh ba tiêu chí: cơ chế tác động, hiệu suất điện hóa và thách thức khi ứng dụng.",
      "II. Tìm kiếm và sàng lọc tài liệu bằng Elicit",
      "Elicit được sử dụng như một công cụ hỗ trợ tìm kiếm và trích xuất dữ liệu từ các tài liệu khoa học liên quan. Công cụ giúp giảm thời gian đọc sơ bộ, nhưng việc lựa chọn tài liệu và diễn giải kết quả vẫn do người học thực hiện.",
      "1. Nhập câu hỏi nghiên cứu bằng các thuật ngữ tiếng Anh chuyên ngành để tăng khả năng tìm đúng tài liệu.",
      "2. Ưu tiên các nghiên cứu mô tả rõ cấu trúc vật liệu, cơ chế giữ polysulfide và kết quả sạc xả.",
      "3. Loại bỏ các kết quả chỉ đề cập chung đến Graphene nhưng không cung cấp dữ liệu liên quan đến cathode hoặc pin Li-S.",
      "4. Chọn các công trình đại diện cho nhiều hướng tiếp cận: GO làm chất neo giữ, rGO làm lớp trung gian, Graphene 3D làm khung dẫn và nghiên cứu cơ chế sau chu kỳ.",
      "III. Xây dựng tiêu chí và bảng so sánh",
      "Các tài liệu được đưa về cùng một cấu trúc để tránh việc so sánh cảm tính. Mỗi nghiên cứu được xem xét theo tác giả và năm, phương pháp, cơ chế tác động, kết quả chính, thông số hiệu suất và hạn chế.",
      "1. Tiêu chí phương pháp cho biết vật liệu Graphene được đặt ở đâu và giữ vai trò gì trong cấu trúc pin.",
      "2. Tiêu chí cơ chế giúp phân biệt khả năng giữ polysulfide bằng liên kết hóa học, rào cản vật lý hoặc mạng dẫn điện.",
      "3. Tiêu chí hiệu suất dùng để xem xét dung lượng, độ ổn định chu kỳ và khả năng duy trì vật liệu hoạt tính.",
      "4. Tiêu chí hạn chế giúp tránh kết luận một chiều chỉ dựa trên thông số tốt trong phòng thí nghiệm.",
      "IV. Phân tích Graphene oxide làm chất neo giữ",
      "Nghiên cứu của Ji và cộng sự sử dụng Graphene oxide trong cathode để giữ sulfur và polysulfide. Các nhóm chức chứa oxy trên GO tạo tương tác hóa học và phân cực mạnh, nhờ đó hạn chế vật liệu hoạt tính hòa tan vào chất điện phân.",
      "1. Điểm mạnh: cải thiện dung lượng đảo ngược và độ ổn định sạc xả nhờ khả năng giữ polysulfide.",
      "2. Hạn chế: mạng carbon bị gián đoạn bởi các nhóm chức oxy khiến GO có độ dẫn điện thấp hơn Graphene nguyên bản.",
      "3. Nhận xét: GO có lợi về mặt tương tác hóa học nhưng cần được kết hợp với vật liệu dẫn điện để cân bằng hiệu suất.",
      "V. Phân tích rGO và cấu trúc Graphene 3D",
      "Các nghiên cứu về rGO và Graphene 3D chuyển trọng tâm từ liên kết hóa học sang thiết kế cấu trúc dẫn điện và rào cản vật lý.",
      "1. Lớp trung gian rGO vừa hạn chế polysulfide khuếch tán, vừa hỗ trợ tái sử dụng vật liệu hoạt tính; nghiên cứu ghi nhận khả năng duy trì khoảng 95% dung lượng ban đầu sau 400 chu kỳ.",
      "2. Nhược điểm của lớp trung gian là làm tăng độ dày, điện trở nội và khối lượng không hoạt tính của viên pin.",
      "3. Khung Graphene 3D tạo mạng dẫn điện, diện tích bề mặt lớn và không gian xốp để chứa sulfur.",
      "4. Tuy nhiên, lực giữ chủ yếu mang tính vật lý nên polysulfide vẫn có thể rò rỉ trong điều kiện vận hành dài hạn.",
      "VI. Đánh giá cơ chế suy giảm và thách thức",
      "Phân tích sau chu kỳ cho thấy vật liệu nền GO có thể tham gia phản ứng phụ với chất điện phân hệ ether, tạo sản phẩm phụ cách điện và hình thành lớp chặn trên bề mặt điện cực.",
      "1. Hiệu suất Coulombic ban đầu cao không đồng nghĩa với độ bền dài hạn nếu phản ứng phụ tiếp tục tích lũy.",
      "2. Kết quả phòng thí nghiệm trên 1200 mAh/g cần được xem xét cùng mật độ sulfur, lượng chất điện phân và điều kiện thử nghiệm.",
      "3. Chi phí Graphene chất lượng cao và khả năng kiểm soát cấu trúc ở quy mô công nghiệp vẫn là rào cản lớn.",
      "VII. Kết luận và bài học kinh nghiệm",
      "Các tài liệu cho thấy Graphene và các dẫn xuất có thể giải quyết đồng thời hai điểm yếu quan trọng của pin Li-S: độ dẫn điện kém của sulfur và hiệu ứng con thoi của polysulfide. Xu hướng nghiên cứu đang chuyển từ trộn vật liệu đơn giản sang thiết kế cấu trúc nano, màng trung gian và vật liệu pha tạp có chức năng rõ ràng.",
      "Về phương pháp, sự kết hợp giữa mô phỏng tính toán và thực nghiệm hóa lý giúp giải thích cơ chế tương tác ở cấp độ nguyên tử trước khi chế tạo.",
      "Bài học cốt lõi: AI giúp tăng tốc việc tìm và trích xuất tài liệu, nhưng chất lượng tổng quan phụ thuộc vào câu hỏi nghiên cứu, tiêu chí so sánh và khả năng kiểm chứng, tổng hợp của người học.",
    ],
    sections: [
      {
        title: "Câu hỏi nghiên cứu",
        body:
          "Việc tích hợp Graphene oxide (GO) và Reduced Graphene oxide (rGO) vào cấu trúc cathode giúp cải thiện dung lượng lưu trữ và hạn chế hiệu ứng con thoi của polysulfide trong pin Lithium-Sulfur như thế nào?",
      },
      {
        title: "Quy trình thực hiện",
        bullets: [
          "Xác định chủ đề, phạm vi vật liệu và câu hỏi nghiên cứu đủ cụ thể.",
          "Dùng Elicit để tìm và trích xuất các tài liệu liên quan.",
          "Đối chiếu từng nghiên cứu theo cơ chế tác động, kết quả, thông số hiệu suất và hạn chế.",
          "Tổng hợp xu hướng công nghệ, phương pháp luận và rào cản triển khai thực tế.",
        ],
      },
    ],
    findings: [
      {
        title: "GO giữ polysulfide",
        text: "Các nhóm chức chứa oxy tạo tương tác mạnh với sulfur và polysulfide, nhưng làm giảm độ dẫn điện của vật liệu.",
      },
      {
        title: "rGO làm lớp trung gian",
        text: "Lớp rGO vừa cản khuếch tán polysulfide vừa hỗ trợ tái sử dụng vật liệu hoạt tính; nghiên cứu ghi nhận khả năng duy trì 95% dung lượng sau 400 chu kỳ.",
      },
      {
        title: "Graphene 3D tạo khung dẫn",
        text: "Cấu trúc xốp cung cấp mạng dẫn điện, không gian chứa sulfur và khả năng hấp thụ biến đổi thể tích khi sạc xả.",
      },
      {
        title: "Rào cản còn lại",
        text: "Chi phí vật liệu, phản ứng phụ, điện trở nội và khả năng kiểm soát quy trình ở quy mô công nghiệp vẫn là thách thức lớn.",
      },
    ],
    table: {
      targetHeading: "Xây dựng tiêu chí và bảng so sánh",
      caption: "Bảng đối chiếu các hướng ứng dụng Graphene trong pin Lithium-Sulfur",
      columns: ["Hướng tiếp cận", "Giá trị chính", "Hạn chế cần lưu ý"],
      rows: [
        ["GO làm chất neo giữ", "Khóa sulfur và polysulfide bằng tương tác hóa học", "Độ dẫn điện thấp hơn Graphene nguyên bản"],
        ["Màng trung gian rGO", "Hạn chế khuếch tán và kéo dài tuổi thọ chu kỳ", "Tăng độ dày, điện trở nội và khối lượng pin"],
        ["Khung Graphene 3D", "Tăng dẫn điện, diện tích bề mặt và khả năng chứa sulfur", "Liên kết vật lý có thể chưa đủ bền ở điều kiện dài hạn"],
        ["Phân tích cơ chế sau chu kỳ", "Làm rõ phản ứng phụ và nguyên nhân suy giảm dung lượng", "Có thể xuất hiện lớp sản phẩm phụ cách điện"],
      ],
    },
    reflection:
      "Bài tập giúp em nhận ra AI có thể rút ngắn đáng kể bước sàng lọc tài liệu, nhưng kết quả chỉ có giá trị khi người học đặt câu hỏi rõ, so sánh nhiều tiêu chí và tự tổng hợp thay vì chấp nhận câu trả lời có sẵn.",
  },
  "bai-3": {
    title: "Bài 3: Viết Prompt hiệu quả cho các tác vụ học thuật",
    summary:
      "Thử nghiệm ba cấp độ prompt trên ba tác vụ học tập để chứng minh rằng vai trò, ngữ cảnh, đối tượng và định dạng đầu ra quyết định trực tiếp đến chất lượng câu trả lời của AI.",
    tags: ["Gemini", "Prompt Engineering", "C.R.E.A.T.E"],
    metrics: [
      ["Tác vụ thử nghiệm", "3"],
      ["Cấp độ prompt", "Cơ bản → nâng cao"],
      ["Khung đúc kết", "C.R.E.A.T.E"],
    ],
    sections: [
      {
        title: "Ba tác vụ được lựa chọn",
        bullets: [
          "Tóm tắt tài liệu học thuật về tác động của AI trong giáo dục.",
          "Giải thích khái niệm Blockchain cho người mới bằng phép liên tưởng.",
          "Tạo bộ câu hỏi ôn tập về lập kế hoạch kinh doanh.",
        ],
      },
      {
        title: "Điều thay đổi ở prompt nâng cao",
        bullets: [
          "Role Prompting định hình đúng giọng văn và mức độ chuyên môn.",
          "Constraint kiểm soát độ dài, cấu trúc và trọng tâm của đầu ra.",
          "Analogy giúp chuyển khái niệm trừu tượng thành ví dụ gần gũi.",
          "Few-shot cung cấp khuôn mẫu để AI tạo câu hỏi có chiều sâu hơn.",
        ],
      },
    ],
    findings: [
      {
        title: "Tóm tắt dễ học hơn",
        text: "Prompt nâng cao tạo đầu ra có gạch đầu dòng, lợi ích cốt lõi và ví dụ phù hợp với sinh viên năm nhất.",
      },
      {
        title: "Khái niệm bớt trừu tượng",
        text: "Phép liên tưởng cuốn sổ điểm dùng chung giúp mô tả tính phân quyền và chống sửa đổi của Blockchain trực quan hơn.",
      },
      {
        title: "Câu hỏi có chiều sâu",
        text: "Few-shot chuyển bộ câu hỏi từ ghi nhớ khái niệm sang phân tích tình huống và giải thích bẫy sai logic.",
      },
    ],
    table: {
      targetHeading: "Nguyên tắc viết prompt hiệu quả trong học tập",
      caption: "Bảng thành phần của khung viết prompt C.R.E.A.T.E",
      columns: ["Thành phần", "Ý nghĩa trong khung C.R.E.A.T.E"],
      rows: [
        ["Context", "Cung cấp hoàn cảnh và dữ kiện cần thiết"],
        ["Role", "Chỉ định vai trò chuyên gia phù hợp"],
        ["Examples", "Đưa mẫu để AI học cấu trúc mong muốn"],
        ["Audience", "Xác định rõ người đọc của đầu ra"],
        ["Task & Tone", "Nêu nhiệm vụ, các bước và giọng điệu"],
        ["Explicit Format", "Quy định định dạng, độ dài và cách trình bày"],
      ],
    },
    tables: [
      {
        targetHeading: "So sánh các phiên bản prompt",
        caption: "Bảng so sánh mức độ chi tiết và chất lượng đầu ra của ba phiên bản prompt",
        columns: ["Phiên bản", "Đặc điểm câu lệnh", "Chất lượng đầu ra"],
        rows: [
          ["Prompt cơ bản", "Câu lệnh ngắn, ít ngữ cảnh và ít ràng buộc", "Thường chung chung, dễ lan man và chưa sát nhu cầu học tập"],
          ["Prompt cải tiến", "Bổ sung yêu cầu về độ dài, định dạng hoặc đối tượng", "Rõ ràng, dễ đọc hơn nhưng đôi khi vẫn thiếu chiều sâu"],
          ["Prompt nâng cao", "Có vai trò, ngữ cảnh, đối tượng, ví dụ mẫu và yêu cầu cụ thể", "Có cấu trúc tốt, sát mục tiêu và dễ sử dụng trong học tập"],
        ],
      },
    ],
    reflection:
      "Prompt Engineering không chỉ là ra lệnh cho máy. Quá trình viết prompt buộc em phải diễn đạt mục tiêu rõ ràng, suy nghĩ có cấu trúc và biết đánh giá đầu ra theo tiêu chí cụ thể.",
  },
  "bai-4": {
    title: "Bài 4: Cộng tác trực tuyến cho dự án nhóm",
    summary:
      "Trong vai trò người khởi xướng và điều phối, em kết hợp Trello, Google Docs và Discord để quản lý dự án nhóm về tổng quan Trí tuệ nhân tạo, kiểm soát tiến độ và tránh xung đột phiên bản.",
    tags: ["Trello", "Google Docs", "Discord"],
    metrics: [
      ["Công cụ phối hợp", "3"],
      ["Cập nhật tiến độ", "Ít nhất 3 lần/tuần"],
      ["Tương tác", "Hơn 10 lượt/tuần"],
    ],
    sections: [
      {
        title: "Vai trò và cách tổ chức",
        bullets: [
          "Trello: thiết lập bảng Kanban, nhãn ưu tiên và nhắc lịch tự động.",
          "Google Docs: tổ chức thư mục phân cấp, phân quyền Viewer/Editor và theo dõi lịch sử phiên bản.",
          "Discord: chia kênh theo chủ đề và dùng luồng thảo luận để tránh trôi thông tin.",
          "Tệp được đặt tên theo cấu trúc ngày, nội dung và phiên bản.",
        ],
      },
    ],
    findings: [
      {
        title: "Thông tin được kiểm soát",
        text: "Các yêu cầu quan trọng được ghim và tách luồng, giúp nhóm phản hồi đúng hạn.",
      },
      {
        title: "Không còn nhầm phiên bản",
        text: "Quy tắc đặt tên và Version History loại bỏ tình trạng ghi đè hoặc sử dụng nhầm bản thảo.",
      },
      {
        title: "Tiến độ ổn định hơn",
        text: "Nhắc việc trước hạn 24 giờ giúp các thành viên chủ động hoàn thành nhiệm vụ.",
      },
    ],
    table: {
      targetHeading: "Phân tích thách thức và giải pháp",
      caption: "Bảng phân tích thách thức, giải pháp và kết quả cộng tác trực tuyến",
      columns: ["Thách thức", "Giải pháp", "Kết quả"],
      rows: [
        ["Trôi thông tin", "Ghim nội dung và tạo luồng thảo luận riêng", "Thông tin quan trọng được phản hồi đúng hạn"],
        ["Xung đột phiên bản", "Quy tắc đặt tên và Version History", "Loại bỏ nhầm file và ghi đè"],
        ["Tiến độ không đều", "Nhắc tự động trước hạn 24 giờ", "Cải thiện tỷ lệ hoàn thành đúng hạn"],
      ],
    },
    reflection:
      "Cộng tác trực tuyến hiệu quả không nằm ở việc dùng nhiều công cụ, mà ở cách phân vai rõ ràng, thống nhất quy tắc và duy trì nhịp cập nhật đều đặn.",
  },
  "bai-5": {
    title: "Bài 5: Sử dụng AI tạo sinh để sáng tạo nội dung",
    summary:
      "Xây dựng Infographic “Tác động của AI đối với các ngành nghề trong tương lai” bằng quy trình kết hợp Gemini, DALL-E 3 và Canva, trong đó AI cung cấp nguyên liệu còn con người kiểm định và hoàn thiện.",
    tags: ["Gemini", "DALL-E 3", "Canva"],
    metrics: [
      ["Sản phẩm", "Infographic"],
      ["Công cụ chính", "3"],
      ["Lượt tinh chỉnh ảnh", "2 phiên bản prompt"],
    ],
    sections: [
      {
        title: "Quy trình sáng tạo",
        bullets: [
          "Gemini xây dựng dàn ý 5 phần dành cho sinh viên và người đi làm.",
          "DALL-E 3 tạo hình ảnh biểu tượng về con người làm việc cùng AI.",
          "Canva Magic Design gợi ý bố cục ban đầu.",
          "Font, màu sắc, biểu tượng và khoảng 30% văn bản được chỉnh thủ công để phù hợp ngữ cảnh học thuật.",
        ],
      },
      {
        title: "Từ prompt chung đến prompt có chủ đích",
        body:
          "Prompt “A futuristic office” cho hình ảnh quá chung chung. Sau khi bổ sung bối cảnh văn phòng tương lai, ánh sáng điện ảnh, phong cách 3D, màu sắc và tương tác người–robot, đầu ra phù hợp với phong cách Infographic hơn.",
      },
    ],
    findings: [
      {
        title: "Tăng tốc giai đoạn ý tưởng",
        text: "AI giúp mở rộng góc nhìn và rút ngắn thời gian xây dựng nguyên liệu ban đầu.",
      },
      {
        title: "Dấu ấn cá nhân vẫn quyết định",
        text: "Bố cục, bảng màu, lựa chọn thông tin và khâu kiểm định là phần tạo nên chất lượng sản phẩm cuối cùng.",
      },
      {
        title: "Cần kiểm soát bản quyền",
        text: "Hình ảnh do AI tạo ra đặt ra yêu cầu minh bạch về công cụ và thận trọng khi xác định quyền sở hữu.",
      },
    ],
    reflection:
      "Em chuyển từ quy trình làm việc tuần tự sang cộng tác có kiểm soát với AI: máy gợi ý nhiều phương án, còn em chịu trách nhiệm chọn lọc, chỉnh sửa và bảo đảm thông điệp cuối cùng.",
  },
  "bai-6": {
    title: "Bài 6: Sử dụng AI có trách nhiệm trong học tập",
    summary:
      "So sánh định hướng sử dụng AI trong học thuật, ghi lại quá trình dùng Gemini và Canva, phân tích ranh giới hỗ trợ–gian lận và xây dựng bộ 7 nguyên tắc cá nhân.",
    tags: ["Liêm chính học thuật", "70/30", "Kiểm chứng"],
    metrics: [
      ["Chính sách đối chiếu", "VNU-UET và Stanford"],
      ["Nguyên tắc cá nhân", "7"],
      ["Tỷ lệ định hướng", "70% người học / 30% AI"],
    ],
    sections: [
      {
        title: "Nhận định về chính sách",
        body:
          "Cả hai môi trường đều không xem AI là công cụ phải cấm tuyệt đối, nhưng yêu cầu người học minh bạch, tuân thủ quy định từng học phần và chịu trách nhiệm với nội dung nộp.",
      },
      {
        title: "Ba rủi ro cần kiểm soát",
        bullets: [
          "Gian lận khi nộp nguyên nội dung AI tạo ra mà không kiểm chứng hoặc công khai.",
          "Vấn đề sở hữu trí tuệ khi đầu ra được tạo từ kho dữ liệu lớn.",
          "Sự lệ thuộc làm suy giảm tư duy phản biện và khả năng viết độc lập.",
        ],
      },
    ],
    findings: [
      { title: "70/30", text: "AI chỉ hỗ trợ phần nguyên liệu; tư duy, kiểm chứng và quyết định cuối cùng thuộc về người học." },
      { title: "Kiểm chứng bắt buộc", text: "Số liệu, ngày tháng và khẳng định quan trọng phải được đối chiếu với nguồn đáng tin cậy." },
      { title: "Minh bạch", text: "Công khai phần việc có AI hỗ trợ và không cung cấp dữ liệu cá nhân hoặc dữ liệu nội bộ cho công cụ công cộng." },
      { title: "Không rập khuôn", text: "Template và gợi ý từ AI phải được biên tập để phản ánh mục tiêu, ngữ cảnh và dấu ấn cá nhân." },
    ],
    table: {
      targetHeading: "Nghiên cứu chính sách sử dụng AI",
      caption: "Bảng so sánh định hướng sử dụng AI trong học thuật",
      columns: ["Tiêu chí", "VNU-UET", "Stanford"],
      rows: [
        ["Mức độ cho phép", "Hỗ trợ ý tưởng và tối ưu quy trình", "Phụ thuộc quy định cụ thể của từng môn học"],
        ["Minh bạch", "Trích dẫn khi AI đóng góp vào nội dung chính", "Công khai sự hỗ trợ của AI"],
        ["Trọng tâm", "Liêm chính và trung thực học thuật", "Sáng tạo đi cùng trách nhiệm cá nhân"],
      ],
    },
    reflection:
      "Giá trị lớn nhất của bài tập là chuyển vai trò của em từ người nhận câu trả lời sang người kiểm soát công cụ: biết hỏi, biết nghi ngờ, biết sửa và chịu trách nhiệm với sản phẩm cuối cùng.",
  },
};
