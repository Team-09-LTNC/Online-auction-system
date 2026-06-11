window.REPORT_DATA = {
  "bai-1": {
    title: "Bài 1: Thao tác cơ bản với tập tin và thư mục",
    summary:
      "Thực hành trọn vẹn quy trình tạo, đổi tên, sao chép, di chuyển, xóa và khôi phục dữ liệu trong File Explorer. Đây là bài duy nhất sử dụng chuỗi ảnh chụp từng bước để chứng minh thao tác.",
    tags: ["File Explorer", "13 ảnh gốc", "Quản lý dữ liệu"],
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
        text: "Lớp rGO vừa cản khuếch tán polysulfide vừa hỗ trợ tái sử dụng vật liệu hoạt tính; báo cáo ghi nhận khả năng duy trì 95% dung lượng sau 400 chu kỳ.",
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
