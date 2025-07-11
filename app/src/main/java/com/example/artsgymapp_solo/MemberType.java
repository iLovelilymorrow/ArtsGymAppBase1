package com.example.artsgymapp_solo;

public class MemberType
{
    private int id;
    private String name;
    private int durationDays;
    private boolean isTwoInOne;

    public MemberType(int id, String name, int durationDays, boolean isTwoInOne)
    {
        this.id = id;
        this.name = name;
        this.durationDays = durationDays;
        this.isTwoInOne = isTwoInOne;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public boolean isTwoInOne() {
        return isTwoInOne;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getDurationDays() {
        return durationDays;
    }

    // Important for Spinner display
    @Override
    public String toString() {
        return name; // This is what the ArrayAdapter will display in the Spinner
    }
}