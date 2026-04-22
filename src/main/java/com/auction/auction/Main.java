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
        System.out.println("🚀 KHOI DONG SAN DAU GIA!");

        // 1. Dựng rạp, lấy hàng ra bán (Gia khoi diem: 1000)
        Item item = new Item("Buc Tranh Mona Lisa", 1000);
        Auction auction = new Auction(item);

        // 2. Mo san trong 5 giay (Test nhanh)
        auction.startAuction(5);

        // 3. Tha 100 dai gia vao tra gia (Multithreading)
        System.out.println("🔥 100 DAI GIA DANG LAO VAO TRA GIA...");
        for (int i = 1; i <= 100; i++) {
            final int stt = i;
            new Thread(() -> {
                // Tao dai gia
                Bidder daiGia = new Bidder("user" + stt, "123456", "Dai gia so " + stt);

                // Tra gia random tu 1000 den 5000
                double tienBid = 1000 + (Math.random() * 4000);
                BidTransaction tx = new BidTransaction(daiGia, tienBid);

                try {
                    // Gui len san dau gia (Ham nay se tu dong luu xuong MySQL)
                    auction.addValidBid(tx);
                } catch (InvalidBidException ex) {
                    System.getLogger(Main.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }
            }).start();
        }

        // 4. Cho luong chinh (Main Thread) ngu 6 giay de cho san dong cua
        try {
            System.out.println("⏳ Dang cho phien dau gia dong cua...");
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            System.out.println("⚠️ Interrupted while waiting for auction to end!");
        }

        // 5. KIEM TRA HANG TRONG KHO (Read from MySQL)
        System.out.println("\n===============================================");
        System.out.println("=== 🔍 DOC DU LIEU TU MYSQL LEN DE KIEM TRA ===");
        System.out.println("===============================================");

        BidTransactionDAO dao = new BidTransactionDAO(DatabaseConnection.getConnection());

        // ⚠️ CHÚ Ý CHỖ NÀY:
        // Nếu sếp thiết kế auctionId là kiểu số (int) thì để nguyên số 1
        // Nếu sếp thiết kế auctionId là kiểu chữ (String) thì sửa thành "1" nhé!
        dao.printTransactionHistory(1);
    }
}