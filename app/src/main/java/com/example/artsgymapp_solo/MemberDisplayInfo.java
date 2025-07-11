package com.example.artsgymapp_solo; // Your package

import java.time.LocalDate;

public class MemberDisplayInfo {
    // From Member
    private String memberID;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String gender;
    private int age;
    private String imageFilePath;

    // From latest MembershipPeriod
    private int periodId; // pk of the period
    private int fkMemberTypeId;
    private String memberTypeName;
    private LocalDate registrationDate;
    private LocalDate expirationDate;
    private String receiptNumber;
    private boolean isActive; // Calculated or from latest period

    public MemberDisplayInfo(String memberID, String firstName, String lastName, String phoneNumber,
                             String gender, int age, String imageFilePath,
                             int periodId, int fkMemberTypeId, String memberTypeName,
                             LocalDate registrationDate, LocalDate expirationDate, String receiptNumber,
                             boolean isActive) {
        this.memberID = memberID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.age = age;
        this.imageFilePath = imageFilePath;
        this.periodId = periodId;
        this.fkMemberTypeId = fkMemberTypeId;
        this.memberTypeName = memberTypeName;
        this.registrationDate = registrationDate;
        this.expirationDate = expirationDate;
        this.receiptNumber = receiptNumber;
        this.isActive = isActive;
    }

    // --- Getters for all fields ---
    public String getMemberID() { return memberID; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }

    public String getImageFilePath() {
        return imageFilePath;
    }

    public void setImageFilePath(String imageFilePath) {
        this.imageFilePath = imageFilePath;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public int getPeriodId() { return periodId; }
    public int getFkMemberTypeId() { return fkMemberTypeId; }
    public String getMemberTypeName() { return memberTypeName; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public String getReceiptNumber() { return receiptNumber; }
    public boolean isActive() { return isActive; }
    public String getFullName() { return firstName + " " + lastName; }
}