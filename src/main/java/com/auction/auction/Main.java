package com.auction.auction;

import com.auction.exception.InvalidBidException;
import com.auction.model.bid.Auction;
import com.auction.model.bid.BidTransaction;
import com.auction.model.entity.Item;
import com.auction.model.user.Bidder;
import com.auction.repository.BidTransactionDAO;
import com.auction.utils.DatabaseConnection;

public class Main {
    public static void main(String[] args) {
        System.out.println("KHOI DONG SAN DAU GIA!");

        // 1. Dựng rạp, lấy hàng ra bán (Giá khởi điểm: 1000)
        Item item = new Item("Buc Tranh Mona Lisa", 1000);
        Auction auction = new Auction(item);

        // 2. Mở sàn trong 5 giây
        auction.startAuction(5);

        // 3. Thêm 100 người vào trả giá (Multithreading)
        System.out.println("100 Người đang vào trả giá...");
        for (int i = 1; i <= 100; i++) {
            final int stt = i;
            new Thread(() -> {

                Bidder daiGia = new Bidder("user" + stt, "123456", "Người " + stt);

                double tienBid = 1000 + (Math.random() * 4000);
                BidTransaction tx = new BidTransaction(daiGia, tienBid);

                try {
                    // Gửi lên sàn đấu giá(Hàm này tự động lưu lên SQL)
                    auction.addValidBid(tx);
                } catch (InvalidBidException ex) {
                    System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }
            }).start();
        }

        // Cho luồng chính ngủ 6s để phiên đóng cửa
        try {
            System.out.println("Đang đóng cửa phiên đấu giá...");
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            System.out.println("Interrupted while waiting for auction to end!");
        }

        // 5. Kiểm tra (Read from MySQL)
        System.out.println("\n===============================================");
        System.out.println("DOC DU LIEU TU MYSQL LEN DE KIEM TRA ===");
        System.out.println("===============================================");

        BidTransactionDAO dao = new BidTransactionDAO(DatabaseConnection.getConnection());

        dao.printTransactionHistory(1);
    }
}