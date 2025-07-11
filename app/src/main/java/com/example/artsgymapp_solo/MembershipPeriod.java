package com.example.artsgymapp_solo;

import java.time.LocalDate;

public class MembershipPeriod {
    private int periodId; // Corresponds to COLUMN_PERIOD_ID
    private String fkMemberId; // Corresponds to COLUMN_FK_MEMBER_ID_PERIOD
    private int fkMemberTypeId; // Corresponds to COLUMN_FK_MEMBER_TYPE_ID_PERIOD
    private String memberTypeName; // For convenience, if you join and fetch it
    private LocalDate registrationDate; // Corresponds to COLUMN_PERIOD_REGISTRATION_DATE
    private LocalDate expirationDate; // Corresponds to COLUMN_PERIOD_EXPIRATION_DATE
    private String receiptNumber; // Corresponds to COLUMN_PERIOD_RECEIPT_NUMBER

    // Constructors
    public MembershipPeriod() {
        // Default constructor
    }

    public int getPeriodId() {
        return periodId;
    }

    public void setPeriodId(int periodId) {
        this.periodId = periodId;
    }

    public String getFkMemberId() {
        return fkMemberId;
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

    public String getMemberTypeName() {
        return memberTypeName;
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