package com.auction.auction;

import com.auction.exception.InvalidBidException;
import com.auction.model.bid.Auction;
import com.auction.model.bid.BidTransaction;
import com.auction.model.item.Item;
import com.auction.model.item.Art; // Import lớp con Art
import com.auction.model.user.Bidder;
import com.auction.dao.BidTransactionDAO;
import com.auction.utils.DatabaseConnection;

public class Main {
    public static void main(String[] args) {
        System.out.println("KHOI DONG SAN DAU GIA!");

        // 1. Khởi tạo vật phẩm cụ thể (Art) và phiên đấu giá
        // Lưu ý: Các tham số truyền vào hàm new Art(...) có thể thay đổi tùy thuộc vào cấu trúc class Art của team bạn
        Item item = new Art("Buc Tranh Mona Lisa", "Bản gốc", 1000.0, "Leonardo da Vinci", 1503, "Sơn dầu");
        Auction auction = new Auction(item);

        // 2. Mở sàn trong 5 giây (chạy ngầm trong ExecutorService của Auction)
        auction.startAuction(5);

        // 3. Giả lập 100 người dùng đồng loạt trả giá (Multithreading)
        System.out.println("100 Người đang vào trả giá...");
        for (int i = 1; i <= 100; i++) {
            final int stt = i;
            new Thread(() -> {
                Bidder daiGia = new Bidder("user" + stt, "123456", "Người " + stt);
                double tienBid = 1000 + (Math.random() * 4000);
                BidTransaction tx = new BidTransaction(daiGia, tienBid);

                try {
                    // Cố gắng đặt giá, sẽ bị throw InvalidBidException nếu giá đưa ra thấp hơn giá cao nhất hiện tại
                    auction.addValidBid(tx);
                } catch (InvalidBidException ex) {
                    // Bỏ qua log để console không bị nhiễu do có quá nhiều luồng bị từ chối giá
                }
            }).start();
        }

        // 4. Thread chính chờ 6s để đảm bảo phiên đấu giá đã đóng hoàn toàn (đóng ở giây thứ 5)
        try {
            System.out.println("Đang đợi phiên đấu giá kết thúc...");
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            System.out.println("Interrupted while waiting for auction to end!");
        }

        // 5. Kiểm tra tính nhất quán dữ liệu từ Database
        System.out.println("\n===============================================");
        System.out.println("DOC DU LIEU TU MYSQL LEN DE KIEM TRA");
        System.out.println("===============================================");

        BidTransactionDAO dao = new BidTransactionDAO(DatabaseConnection.getConnection());
        dao.printTransactionHistory(1);

        // Thoái chương trình an toàn
        System.exit(0);
    }
}