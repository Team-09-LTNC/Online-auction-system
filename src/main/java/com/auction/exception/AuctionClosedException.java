package com.auction.exception;

public class AuctionClosedException extends Exception{
    public AuctionClosedException(String msg){
        super(msg);
    }
}
