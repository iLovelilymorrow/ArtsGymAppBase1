package com.example.artsgymapp_solo;

import java.time.LocalDate;

public class MembershipPeriod {
    private int periodId; 
    private String fkMemberId; 
    private int fkMemberTypeId; 
    private String memberTypeName; 
    private LocalDate registrationDate; 
    private LocalDate expirationDate; 
    private String receiptNumber; 

    
    public MembershipPeriod() {
        
    }

    public int getPeriodId() {
        return periodId;
    }

    public void setPeriodId(int periodId) {
        this.periodId = periodId;
    }

    public void setFkMemberId(String fkMemberId) {
        this.fkMemberId = fkMemberId;
    }

    public int getFkMemberTypeId() {
        return fkMemberTypeId;
    }

    public void setFkMemberTypeId(int fkMemberTypeId) {
        this.fkMemberTypeId = fkMemberTypeId;
    }

    public void setMemberTypeName(String memberTypeName) {
        this.memberTypeName = memberTypeName;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    @Override
    public String toString()
    {
        return "MembershipPeriod{" +
                "periodId=" + periodId +
                ", fkMemberId='" + fkMemberId + '\'' +
                ", fkMemberTypeId=" + fkMemberTypeId +
                ", memberTypeName='" + memberTypeName + '\'' +
                ", registrationDate=" + registrationDate +
                ", expirationDate=" + expirationDate +
                ", receiptNumber='" + receiptNumber + '\'' +
                '}';
    }
}