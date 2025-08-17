package com.example.artsgymapp_solo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class MemberDisplayInfo {

    private String memberID;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String gender;
    private int age;
    private String imageFilePath;

    private int periodId;
    private int fkMemberTypeId;
    private String memberTypeName;
    private LocalDate registrationDate;
    private LocalDate expirationDate;
    private String receiptNumber;
    private boolean isActive;

    public enum MembershipStatus {
        ACTIVE,         // For active members not yet expiring soon
        EXPIRING_SOON,  // For members whose expiration is within a defined window
        EXPIRED         // For members whose membership has passed its expiration date
    }

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

    // --- Getters ---
    public String getMemberID() { return memberID; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    public String getImageFilePath() { return imageFilePath; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getPhoneNumber() { return phoneNumber; }
    public int getPeriodId() { return periodId; }
    public String getMemberTypeName() { return memberTypeName; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public String getReceiptNumber() { return receiptNumber; }
    public boolean isActive() { return isActive; }

    // --- Setters ---
    public void setAge(int age) { this.age = age; }
    public void setGender(String gender) { this.gender = gender; }

    // --- Method to determine current membership status ---
    public MembershipStatus getCurrentMembershipStatus() {
        LocalDate today = LocalDate.now();

        // Optional: Incorporate this.isActive if it provides overriding logic
        // if (!this.isActive) {
        //     return MembershipStatus.EXPIRED; // Or a specific INACTIVE status
        // }

        // 1. Handle cases with no expiration date
        if (getExpirationDate() == null) {
            // If it never expires, it's considered 'ACTIVE'.
            return MembershipStatus.ACTIVE;
        }

        // 2. Check if expired
        if (getExpirationDate().isBefore(today)) {
            return MembershipStatus.EXPIRED;
        }

        // 3. Check if expiring soon
        long daysUntilExpiration = ChronoUnit.DAYS.between(today, getExpirationDate());
        if (daysUntilExpiration <= 5) { // Expires within the next 5 days (inclusive of today)
            return MembershipStatus.EXPIRING_SOON;
        }

        // 4. If none of the above, it's considered Active
        return MembershipStatus.ACTIVE;
    }
}
