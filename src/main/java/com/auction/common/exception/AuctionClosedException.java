package com.auction.common.exception;

public class AuctionClosedException extends Exception{
    public AuctionClosedException(String msg){
        super(msg);
    }
}
