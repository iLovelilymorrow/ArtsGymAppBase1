package com.example.artsgymapp_solo;

public class Member {
    private String memberID;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String gender;
    private int age;
    private String imageFilePath;
    private byte[] fingerprintTemplate;

    public Member()
    {

    }

    public Member(String memberID, String firstName, String lastName, String phoneNumber,
                  String gender, int age, String imageFilePath) {
        this.memberID = memberID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.age = age;
        this.imageFilePath = imageFilePath;
    }

    public String getMemberID() {
        return memberID;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getImageFilePath() {
        return imageFilePath;
    }

    public void setImageFilePath(String imageFilePath) {
        this.imageFilePath = imageFilePath;
    }
    public byte[] getFingerprintTemplate() {
        return fingerprintTemplate;
    }
    public void setFingerprintTemplate(byte[] fingerprintTemplate) {
        this.fingerprintTemplate = fingerprintTemplate;
    }
    @Override
    public String toString() {
        return "Member{" +
                "memberID='" + memberID + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", gender='" + gender + '\'' +
                ", age=" + age +
                ", imageFilePath='" + imageFilePath + '\'' +
                '}';
    }
}