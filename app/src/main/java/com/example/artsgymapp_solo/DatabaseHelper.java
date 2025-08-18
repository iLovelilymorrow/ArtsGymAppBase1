package com.example.artsgymapp_solo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    public static final String DATABASE_NAME = "ArtsGymDB";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_MEMBERS = "Members";
    private static final String COLUMN_MEMBER_ID = "MemberID";
    private static final String COLUMN_FIRSTNAME = "FirstName";
    private static final String COLUMN_LASTNAME = "LastName";
    private static final String COLUMN_PHONE_NUMBER = "PhoneNumber";
    private static final String COLUMN_GENDER = "Gender";
    private static final String COLUMN_AGE = "Age";
    private static final String COLUMN_IMAGE_FILE_PATH = "ImageFilePath";
    public static final String COLUMN_FINGERPRINT_TEMPLATE = "FingerprintTemplate";
    
    private static final String TABLE_MEMBER_TYPES = "MemberTypes";
    private static final String COLUMN_MT_ID = "MemberTypeID"; 
    private static final String COLUMN_MT_NAME = "MemberTypeName"; 
    private static final String COLUMN_MT_DURATION_DAYS = "DurationDays"; 
    private static final String COLUMN_MT_IS_TWO_IN_ONE = "isTwoInOne";
    
    private static final String TABLE_MEMBERSHIP_PERIODS = "MembershipPeriods";
    private static final String COLUMN_PERIOD_ID = "PeriodID"; 
    private static final String COLUMN_FK_MEMBER_ID_PERIOD = "FK_MemberID"; 
    private static final String COLUMN_FK_MEMBER_TYPE_ID_PERIOD = "FK_MemberTypeID"; 
    private static final String COLUMN_PERIOD_REGISTRATION_DATE = "RegistrationDate"; 
    private static final String COLUMN_PERIOD_EXPIRATION_DATE = "ExpirationDate"; 
    private static final String COLUMN_PERIOD_RECEIPT_NUMBER = "ReceiptNumber";

    private static final DateTimeFormatter SQLITE_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createMemberTypesTable = "CREATE TABLE " + TABLE_MEMBER_TYPES + "(" +
                COLUMN_MT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_MT_NAME + " TEXT UNIQUE NOT NULL," +
                COLUMN_MT_DURATION_DAYS + " INTEGER NOT NULL," +
                COLUMN_MT_IS_TWO_IN_ONE + " INTEGER NOT NULL DEFAULT 0" +
                ")";

        sqLiteDatabase.execSQL(createMemberTypesTable);

        addInitialMemberType(sqLiteDatabase, "Regular", 30, 0);
        addInitialMemberType(sqLiteDatabase, "Student", 30, 0);

        String createMembersTable = "CREATE TABLE " + TABLE_MEMBERS + "(" +
                COLUMN_MEMBER_ID + " TEXT PRIMARY KEY," +
                COLUMN_FIRSTNAME + " TEXT," +
                COLUMN_LASTNAME + " TEXT," +
                COLUMN_PHONE_NUMBER + " TEXT," +
                COLUMN_GENDER + " TEXT," +
                COLUMN_AGE + " INTEGER," +
                COLUMN_IMAGE_FILE_PATH + " TEXT," +
                COLUMN_FINGERPRINT_TEMPLATE + " BLOB" + 
                ")";
        sqLiteDatabase.execSQL(createMembersTable);

        String createMembershipPeriodsTable = "CREATE TABLE " + TABLE_MEMBERSHIP_PERIODS + "(" +
                COLUMN_PERIOD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_FK_MEMBER_ID_PERIOD + " TEXT NOT NULL," +
                COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " INTEGER NOT NULL," +
                COLUMN_PERIOD_REGISTRATION_DATE + " TEXT NOT NULL," +
                COLUMN_PERIOD_EXPIRATION_DATE + " TEXT NOT NULL," +
                COLUMN_PERIOD_RECEIPT_NUMBER + " TEXT," +
                "FOREIGN KEY(" + COLUMN_FK_MEMBER_ID_PERIOD + ") REFERENCES " + TABLE_MEMBERS + "(" + COLUMN_MEMBER_ID + ") ON DELETE CASCADE," +
                "FOREIGN KEY(" + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + ") REFERENCES " + TABLE_MEMBER_TYPES + "(" + COLUMN_MT_ID + ") ON DELETE RESTRICT" +
                ")";
        sqLiteDatabase.execSQL(createMembershipPeriodsTable);
    }

    private void addInitialMemberType(SQLiteDatabase db, String name, int durationDays, int isTwoInOne) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MT_NAME, name);
        values.put(COLUMN_MT_DURATION_DAYS, durationDays);
        values.put(COLUMN_MT_IS_TWO_IN_ONE, isTwoInOne);
        db.insert(TABLE_MEMBER_TYPES, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int previousVersion, int newVersion) {
        
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_MEMBERSHIP_PERIODS);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_MEMBERS);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_MEMBER_TYPES);
        onCreate(sqLiteDatabase);
    }

    public boolean addMemberType(String name, int durationDays, boolean isTwoInOne) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MT_NAME, name);
        values.put(COLUMN_MT_DURATION_DAYS, durationDays);
        values.put(COLUMN_MT_IS_TWO_IN_ONE, isTwoInOne ? 1 : 0);

        long result = db.insert(TABLE_MEMBER_TYPES, null, values);
        
        return result != -1;
    }

    public List<MemberType> getAllMemberTypes() {
        List<MemberType> memberTypes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_MEMBER_TYPES, new String[]{COLUMN_MT_ID, COLUMN_MT_NAME, COLUMN_MT_DURATION_DAYS, COLUMN_MT_IS_TWO_IN_ONE},
                null, null, null, null, COLUMN_MT_NAME + " ASC")) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MT_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MT_NAME));
                    int durationDays = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MT_DURATION_DAYS));
                    boolean isTwoInOne = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MT_IS_TWO_IN_ONE)) == 1;
                    memberTypes.add(new MemberType(id, name, durationDays, isTwoInOne));
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {

        }
        return memberTypes;
    }

    private String insertMemberCore(SQLiteDatabase db, String memberIdToUse,
                                    String firstName, String lastName, String phoneNumber,
                                    String gender, int age, String imageFilePath,
                                    @Nullable byte[] fingerprintTemplate, 
                                    int memberTypeId, LocalDate registrationDate,
                                    LocalDate expirationDate, String receiptNumber) {
        try {
            ContentValues memberValues = new ContentValues();
            memberValues.put(COLUMN_MEMBER_ID, memberIdToUse);
            memberValues.put(COLUMN_FIRSTNAME, firstName);
            memberValues.put(COLUMN_LASTNAME, lastName);
            memberValues.put(COLUMN_PHONE_NUMBER, phoneNumber);
            memberValues.put(COLUMN_GENDER, gender);
            memberValues.put(COLUMN_AGE, age);

            if (imageFilePath != null) {
                memberValues.put(COLUMN_IMAGE_FILE_PATH, imageFilePath);
            } else {
                memberValues.putNull(COLUMN_IMAGE_FILE_PATH);
            }

            
            if (fingerprintTemplate != null) {
                memberValues.put(COLUMN_FINGERPRINT_TEMPLATE, fingerprintTemplate);
            } else {
                memberValues.putNull(COLUMN_FINGERPRINT_TEMPLATE);
            }

            long memberResult = db.insertOrThrow(TABLE_MEMBERS, null, memberValues);

            if (memberResult != -1) {
                boolean periodResult = addInitialMembershipPeriodInternal(db, memberIdToUse, memberTypeId,
                        registrationDate, expirationDate, receiptNumber);

                if (periodResult) {
                    
                    return memberIdToUse;
                } else {
                    
                    return null;
                }
            } else {
                
                return null;
            }
        } catch (SQLException e) {
            
            return null;
        }
    }

    public String addMemberInExternalTransaction(SQLiteDatabase db, String memberIdToUse,
                                                 String firstName, String lastName, String phoneNumber,
                                                 String gender, int age, String imageFilePath,
                                                 @Nullable byte[] fingerprintTemplate, 
                                                 int memberTypeId, LocalDate registrationDate,
                                                 LocalDate expirationDate, String receiptNumber) {
        if (db == null || !db.isOpen() || !db.inTransaction()) {
            
            return null;
        }
        return insertMemberCore(db, memberIdToUse, firstName, lastName, phoneNumber, gender, age, imageFilePath,
                fingerprintTemplate,
                memberTypeId, registrationDate, expirationDate, receiptNumber);
    }

    private boolean addInitialMembershipPeriodInternal(SQLiteDatabase db, String memberId, int memberTypeId,
                                                       LocalDate registrationDate, LocalDate expirationDate,
                                                       String receiptNumber) {
        ContentValues periodValues = new ContentValues();
        periodValues.put(COLUMN_FK_MEMBER_ID_PERIOD, memberId);
        periodValues.put(COLUMN_FK_MEMBER_TYPE_ID_PERIOD, memberTypeId);

        if (registrationDate != null) {
            periodValues.put(COLUMN_PERIOD_REGISTRATION_DATE, registrationDate.format(SQLITE_DATE_FORMATTER));
        } else {
            
            periodValues.putNull(COLUMN_PERIOD_REGISTRATION_DATE);
        }

        if (expirationDate != null) {
            periodValues.put(COLUMN_PERIOD_EXPIRATION_DATE, expirationDate.format(SQLITE_DATE_FORMATTER));
        } else {
            
            periodValues.putNull(COLUMN_PERIOD_EXPIRATION_DATE);
        }

        if (receiptNumber != null && !receiptNumber.isEmpty()) {
            periodValues.put(COLUMN_PERIOD_RECEIPT_NUMBER, receiptNumber);
        } else {
            periodValues.putNull(COLUMN_PERIOD_RECEIPT_NUMBER);
        }

        try {
            long result = db.insertOrThrow(TABLE_MEMBERSHIP_PERIODS, null, periodValues);
            return result != -1;
        } catch (SQLException e)
        {
            return false;
        }
    }

    public boolean renewTwoInOneWithNewPartner(String primaryMemberId,
                                               String newPartnerFirstName, String newPartnerLastName,
                                               String newPartnerPhoneNumber, String newPartnerGender,
                                               int newPartnerAge, String newPartnerImageFilePath,
                                               @Nullable byte[] newPartnerFingerprintTemplate,
                                               int memberTypeId, LocalDate registrationDate,
                                               LocalDate expirationDate, String receiptNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        String newPartnerId = null;

        try {
            newPartnerId = generateNewMemberId(db);
            if (newPartnerId == null) {
                
                return false;
            }

            ContentValues partnerValues = new ContentValues();
            partnerValues.put(COLUMN_MEMBER_ID, newPartnerId);
            partnerValues.put(COLUMN_FIRSTNAME, newPartnerFirstName);
            partnerValues.put(COLUMN_LASTNAME, newPartnerLastName);
            partnerValues.put(COLUMN_PHONE_NUMBER, newPartnerPhoneNumber);
            partnerValues.put(COLUMN_GENDER, newPartnerGender);
            partnerValues.put(COLUMN_AGE, newPartnerAge);

            if (newPartnerImageFilePath != null) {
                partnerValues.put(COLUMN_IMAGE_FILE_PATH, newPartnerImageFilePath);
            } else {
                partnerValues.putNull(COLUMN_IMAGE_FILE_PATH);
            }

            if (newPartnerFingerprintTemplate != null) {
                partnerValues.put(COLUMN_FINGERPRINT_TEMPLATE, newPartnerFingerprintTemplate);
            } else {
                partnerValues.putNull(COLUMN_FINGERPRINT_TEMPLATE);
            }

            long partnerInsertResult = db.insertOrThrow(TABLE_MEMBERS, null, partnerValues);
            if (partnerInsertResult == -1) {
                
                return false;
            }
            

            boolean primaryPeriodAdded = addInitialMembershipPeriodInternal(db, primaryMemberId, memberTypeId,
                    registrationDate, expirationDate, receiptNumber);

            boolean partnerPeriodAdded = false;
            if (primaryPeriodAdded) {
                partnerPeriodAdded = addInitialMembershipPeriodInternal(db, newPartnerId, memberTypeId,
                        registrationDate, expirationDate, receiptNumber);
            }

            if (primaryPeriodAdded && partnerPeriodAdded) {
                db.setTransactionSuccessful();
                success = true;
                
            }
        } catch (SQLException e) {
            success = false;
        } finally {
            db.endTransaction();
        }
        return success;
    }

    public boolean updateMemberAndPeriod(Member member, MembershipPeriod period) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        try {
            ContentValues memberValues = new ContentValues();
            memberValues.put(COLUMN_FIRSTNAME, member.getFirstName());
            memberValues.put(COLUMN_LASTNAME, member.getLastName());
            memberValues.put(COLUMN_PHONE_NUMBER, member.getPhoneNumber());
            memberValues.put(COLUMN_GENDER, member.getGender());
            memberValues.put(COLUMN_AGE, member.getAge());
            memberValues.put(COLUMN_IMAGE_FILE_PATH, member.getImageFilePath());
            memberValues.put(COLUMN_FINGERPRINT_TEMPLATE, member.getFingerprintTemplate());

            int memberRowsAffected = db.update(TABLE_MEMBERS, memberValues,
                    COLUMN_MEMBER_ID + " = ?", new String[]{member.getMemberID()});

            ContentValues periodValues = new ContentValues();

            periodValues.put(COLUMN_FK_MEMBER_TYPE_ID_PERIOD, period.getFkMemberTypeId());
            periodValues.put(COLUMN_PERIOD_EXPIRATION_DATE, period.getExpirationDate() != null ? period.getExpirationDate().toString() : null);
            periodValues.put(COLUMN_PERIOD_RECEIPT_NUMBER, period.getReceiptNumber());

            int periodRowsAffected = db.update(TABLE_MEMBERSHIP_PERIODS, periodValues,
                    COLUMN_PERIOD_ID + " = ?", new String[]{String.valueOf(period.getPeriodId())});

            if (memberRowsAffected > 0 && periodRowsAffected > 0) {//awdawd

                db.setTransactionSuccessful();
                success = true;
            }
        }
        catch (Exception e)
        {
            success = false;
        }
        finally {
            db.endTransaction();
        }
        return success;
    }

    public boolean renewSingleMembership(String memberId, int memberTypeId,
                                         LocalDate registrationDate, LocalDate expirationDate,
                                         String receiptNumber) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues periodValues = new ContentValues();
        periodValues.put(COLUMN_FK_MEMBER_ID_PERIOD, memberId);
        periodValues.put(COLUMN_FK_MEMBER_TYPE_ID_PERIOD, memberTypeId);

        if (registrationDate == null || expirationDate == null) {
            
            return false;
        }
        periodValues.put(COLUMN_PERIOD_REGISTRATION_DATE, registrationDate.format(SQLITE_DATE_FORMATTER));
        periodValues.put(COLUMN_PERIOD_EXPIRATION_DATE, expirationDate.format(SQLITE_DATE_FORMATTER));

        if (receiptNumber != null && !receiptNumber.isEmpty()) {
            periodValues.put(COLUMN_PERIOD_RECEIPT_NUMBER, receiptNumber);
        } else {
            periodValues.putNull(COLUMN_PERIOD_RECEIPT_NUMBER);
        }

        long result = -1;
        try {
            result = db.insertOrThrow(TABLE_MEMBERSHIP_PERIODS, null, periodValues);
        } catch (SQLException ignored) {
            
        }
        return result != -1;
    }

    public boolean renewTwoInOneWithExistingPartner(String primaryMemberId, String existingPartnerId,
                                                    int memberTypeId, LocalDate registrationDate,
                                                    LocalDate expirationDate, String receiptNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;

        try {
            boolean primaryPeriodAdded = addInitialMembershipPeriodInternal(db, primaryMemberId, memberTypeId,
                    registrationDate, expirationDate, receiptNumber);

            boolean partnerPeriodAdded = false;
            if (primaryPeriodAdded) {
                partnerPeriodAdded = addInitialMembershipPeriodInternal(db, existingPartnerId, memberTypeId,
                        registrationDate, expirationDate, receiptNumber);
            }

            if (primaryPeriodAdded && partnerPeriodAdded) {
                db.setTransactionSuccessful();
                success = true;
                
            }
        } catch (SQLException e) {
            success = false;
        } finally {
            db.endTransaction();
        }
        return success;
    }

    public List<MemberDisplayInfo> getActiveMembersForDisplay() {
        List<MemberDisplayInfo> memberDisplayInfoList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String todayDateStr = LocalDate.now().format(SQLITE_DATE_FORMATTER);

        String query = "SELECT " +
                "M." + COLUMN_MEMBER_ID + ", " +
                "M." + COLUMN_FIRSTNAME + ", " +
                "M." + COLUMN_LASTNAME + ", " +
                "M." + COLUMN_PHONE_NUMBER + ", " +
                "M." + COLUMN_GENDER + ", " +
                "M." + COLUMN_AGE + ", " +
                "M." + COLUMN_IMAGE_FILE_PATH + ", " +
                "LatestActiveMP." + COLUMN_PERIOD_ID + " AS ActivePeriodID, " +
                "LatestActiveMP." + COLUMN_PERIOD_REGISTRATION_DATE + " AS ActiveRegDate, " +
                "LatestActiveMP." + COLUMN_PERIOD_EXPIRATION_DATE + " AS ActiveExpDate, " +
                "LatestActiveMP." + COLUMN_PERIOD_RECEIPT_NUMBER + " AS ActiveReceipt, " +
                "LatestActiveMP." + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " AS ActiveFkMemberTypeID, " +
                "MT_Active." + COLUMN_MT_NAME + " AS ActiveMemberTypeName " +
                "FROM " + TABLE_MEMBERS + " M " +
                "INNER JOIN (" +
                "    SELECT " + COLUMN_FK_MEMBER_ID_PERIOD + ", MAX(" + COLUMN_PERIOD_ID + ") AS MaxPeriodID " +
                "    FROM " + TABLE_MEMBERSHIP_PERIODS +
                "    WHERE DATE(" + COLUMN_PERIOD_EXPIRATION_DATE + ") >= DATE('" + todayDateStr + "')" +
                "    GROUP BY " + COLUMN_FK_MEMBER_ID_PERIOD +
                ") AS LatestActivePeriodIDs ON M." + COLUMN_MEMBER_ID + " = LatestActivePeriodIDs." + COLUMN_FK_MEMBER_ID_PERIOD + " " +
                "INNER JOIN " + TABLE_MEMBERSHIP_PERIODS + " LatestActiveMP ON " +
                "    LatestActivePeriodIDs." + COLUMN_FK_MEMBER_ID_PERIOD + " = LatestActiveMP." + COLUMN_FK_MEMBER_ID_PERIOD + " " +
                "    AND LatestActivePeriodIDs.MaxPeriodID = LatestActiveMP." + COLUMN_PERIOD_ID + " " +
                "LEFT JOIN " + TABLE_MEMBER_TYPES + " MT_Active ON LatestActiveMP." + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " = MT_Active." + COLUMN_MT_ID + " " +
                "ORDER BY M." + COLUMN_MEMBER_ID + " DESC";

        try {
            cursor = db.rawQuery(query, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        String memberId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEMBER_ID));
                        String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRSTNAME));
                        String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LASTNAME));
                        String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
                        String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
                        int age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE));
                        String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_FILE_PATH));

                        int activePeriodId = cursor.getInt(cursor.getColumnIndexOrThrow("ActivePeriodID"));
                        String regDateStr = cursor.getString(cursor.getColumnIndexOrThrow("ActiveRegDate"));
                        String expDateStr = cursor.getString(cursor.getColumnIndexOrThrow("ActiveExpDate"));
                        String receiptNumber = cursor.getString(cursor.getColumnIndexOrThrow("ActiveReceipt"));
                        int fkMemberTypeId = cursor.getInt(cursor.getColumnIndexOrThrow("ActiveFkMemberTypeID"));
                        String memberTypeName = cursor.getString(cursor.getColumnIndexOrThrow("ActiveMemberTypeName"));

                        LocalDate registrationDate = null;
                        if (regDateStr != null) try {
                            registrationDate = LocalDate.parse(regDateStr, SQLITE_DATE_FORMATTER);
                        } catch (Exception ignored) {

                        }

                        LocalDate expirationDate = null;
                        if (expDateStr != null) try {
                            expirationDate = LocalDate.parse(expDateStr, SQLITE_DATE_FORMATTER);
                        } catch (Exception ignored) {
                        }

                        boolean isActive = (expirationDate != null && !expirationDate.isBefore(LocalDate.now()));

                        MemberDisplayInfo displayInfo = new MemberDisplayInfo(
                                memberId, firstName, lastName, phoneNumber, gender, age, imagePath,
                                activePeriodId,
                                fkMemberTypeId,
                                memberTypeName != null ? memberTypeName : "N/A",
                                registrationDate,
                                expirationDate,
                                receiptNumber,
                                isActive
                        );
                        memberDisplayInfoList.add(displayInfo);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return memberDisplayInfoList;
    }


    public String generateNewMemberId(SQLiteDatabase dbInstance) {
        SQLiteDatabase dbToUse = dbInstance != null && dbInstance.isOpen() ? dbInstance : this.getReadableDatabase();
        boolean closeDbAfter = false;
        if (dbToUse != dbInstance) {
            closeDbAfter = true;
        }

        String newMemberId = "0001";
        Cursor idCursor = null;
        try {
            if (dbToUse == null || !dbToUse.isOpen()) {
                
                return null;
            }
            idCursor = dbToUse.rawQuery("SELECT MAX(CAST(" + COLUMN_MEMBER_ID + " AS INTEGER)) FROM " + TABLE_MEMBERS, null);
            if (idCursor != null && idCursor.moveToFirst()) {
                if (!idCursor.isNull(0)) {
                    int lastMemberIdNum = idCursor.getInt(0);
                    int nextMemberIdNum = lastMemberIdNum + 1;
                    newMemberId = String.format(Locale.US, "%04d", nextMemberIdNum);
                }
            }
        } catch (Exception e) {
            
            return null;
        } finally {
            if (idCursor != null) {
                idCursor.close();
            }
        }
        return newMemberId;
    }

    public Member getMemberById(String memberIdString) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Member member = null;

        String query = "SELECT * FROM " + TABLE_MEMBERS +
                " WHERE " + COLUMN_MEMBER_ID + " = ?";
        try {
            cursor = db.rawQuery(query, new String[]{memberIdString});
            if (cursor != null && cursor.moveToFirst()) {
                String retrievedMemberID = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEMBER_ID));
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRSTNAME));
                String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LASTNAME));
                String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
                int age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_FILE_PATH));

                member = new Member(retrievedMemberID, firstName, lastName, phoneNumber, gender, age, imagePath);

                int fingerprintColumnIndex = cursor.getColumnIndex(COLUMN_FINGERPRINT_TEMPLATE);
                if (fingerprintColumnIndex != -1 && !cursor.isNull(fingerprintColumnIndex)) {
                    member.setFingerprintTemplate(cursor.getBlob(fingerprintColumnIndex));
                } else {
                    member.setFingerprintTemplate(null);
                }
            }
        } catch (Exception ignored) {
            
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return member;
    }

    public boolean deleteMember(String memberId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String imagePathToDelete = null;
        boolean success = false;

        try (Cursor cursor = db.query(
                TABLE_MEMBERS,
                new String[]{COLUMN_IMAGE_FILE_PATH},
                COLUMN_MEMBER_ID + " = ?",
                new String[]{memberId},
                null, null, null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int imagePathColumnIndex = cursor.getColumnIndex(COLUMN_IMAGE_FILE_PATH);
                if (imagePathColumnIndex != -1) {
                    imagePathToDelete = cursor.getString(imagePathColumnIndex);
                }
            }
        } catch (Exception ignored) {

        }

        db.beginTransaction();
        try {
            int periodsDeleted = db.delete(
                    TABLE_MEMBERSHIP_PERIODS,
                    COLUMN_FK_MEMBER_ID_PERIOD + " = ?",
                    new String[]{memberId}
            );
            

            int memberRowsDeleted = db.delete(
                    TABLE_MEMBERS,
                    COLUMN_MEMBER_ID + " = ?",
                    new String[]{memberId}
            );

            if (memberRowsDeleted > 0) {
                db.setTransactionSuccessful();
                success = true;
                
            }

        } catch (Exception e) {
            
            success = false;
        } finally {
            db.endTransaction();
        }

        if (success && imagePathToDelete != null && !imagePathToDelete.isEmpty()) {
            try {
                File imageFile = new File(imagePathToDelete);
                if (imageFile.exists()) {
                    imageFile.delete();
                }
            } catch (Exception ignored) {
                
            }
        }
        return success;
    }

    public MembershipPeriod getMembershipPeriodById(int periodId) {
        SQLiteDatabase db = this.getReadableDatabase();
        MembershipPeriod period = null;
        try (Cursor cursor = db.query(TABLE_MEMBERSHIP_PERIODS, null, COLUMN_PERIOD_ID + "=?",
                new String[]{String.valueOf(periodId)}, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                period = new MembershipPeriod();
                period.setPeriodId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PERIOD_ID)));
                period.setFkMemberId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FK_MEMBER_ID_PERIOD)));
                period.setFkMemberTypeId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FK_MEMBER_TYPE_ID_PERIOD)));

                String regDateStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PERIOD_REGISTRATION_DATE));
                if (regDateStr != null) period.setRegistrationDate(LocalDate.parse(regDateStr));

                String expDateStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PERIOD_EXPIRATION_DATE));
                if (expDateStr != null) period.setExpirationDate(LocalDate.parse(expDateStr));

                period.setReceiptNumber(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PERIOD_RECEIPT_NUMBER)));
            }
        } catch (Exception ignored) {

        }
        return period;
    }

    public List<String> getMemberIdsByReceiptNumber(String receiptNumber) {
        List<String> memberIds = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();


        String query = "SELECT DISTINCT " + COLUMN_FK_MEMBER_ID_PERIOD +
                " FROM " + TABLE_MEMBERSHIP_PERIODS +
                " WHERE " + COLUMN_PERIOD_RECEIPT_NUMBER + " LIKE ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{"%" + receiptNumber + "%"})) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    memberIds.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FK_MEMBER_ID_PERIOD)));
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {

        }
        
        return memberIds;
    }

    public List<MemberDisplayInfo> getMembershipsStartedInLastThreeMonthsForDisplay(int registrationMonthFilter) {
        List<MemberDisplayInfo> membersList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        LocalDate today = LocalDate.now();
        LocalDate firstDayOfLookbackWindow = today.minusMonths(2).withDayOfMonth(1);
        String startDateToKeepStr = firstDayOfLookbackWindow.format(SQLITE_DATE_FORMATTER);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT ")
                .append("M.").append(COLUMN_MEMBER_ID).append(", ")
                .append("M.").append(COLUMN_FIRSTNAME).append(", ")
                .append("M.").append(COLUMN_LASTNAME).append(", ")
                .append("M.").append(COLUMN_PHONE_NUMBER).append(", ")
                .append("M.").append(COLUMN_GENDER).append(", ")
                .append("M.").append(COLUMN_AGE).append(", ")
                .append("M.").append(COLUMN_IMAGE_FILE_PATH).append(", ")
                .append("MP.").append(COLUMN_PERIOD_ID).append(" AS PeriodID, ")
                .append("MP.").append(COLUMN_PERIOD_REGISTRATION_DATE).append(" AS RegDate, ")
                .append("MP.").append(COLUMN_PERIOD_EXPIRATION_DATE).append(" AS ExpDate, ")
                .append("MP.").append(COLUMN_PERIOD_RECEIPT_NUMBER).append(" AS Receipt, ")
                .append("MP.").append(COLUMN_FK_MEMBER_TYPE_ID_PERIOD).append(" AS FkMemberTypeID, ")
                .append("MT.").append(COLUMN_MT_NAME).append(" AS MemberTypeName ")
                .append("FROM ").append(TABLE_MEMBERS).append(" M ")
                .append("INNER JOIN ").append(TABLE_MEMBERSHIP_PERIODS).append(" MP ON M.")
                .append(COLUMN_MEMBER_ID).append(" = MP.").append(COLUMN_FK_MEMBER_ID_PERIOD).append(" ")
                .append("LEFT JOIN ").append(TABLE_MEMBER_TYPES).append(" MT ON MP.")
                .append(COLUMN_FK_MEMBER_TYPE_ID_PERIOD).append(" = MT.").append(COLUMN_MT_ID).append(" ");

        List<String> selectionArgsList = new ArrayList<>();

        queryBuilder.append("WHERE DATE(MP.").append(COLUMN_PERIOD_REGISTRATION_DATE).append(") >= DATE(?) ");
        selectionArgsList.add(startDateToKeepStr);

        if (registrationMonthFilter > 0 && registrationMonthFilter <= 12) {
            // Add month filtering. SQLite's strftime('%m', date) returns month as '01', '02', ..., '12'
            String monthString = String.format(Locale.US, "%02d", registrationMonthFilter);
            queryBuilder.append("AND strftime('%m', MP.").append(COLUMN_PERIOD_REGISTRATION_DATE).append(") = ? ");
            selectionArgsList.add(monthString);
        }

        queryBuilder.append("ORDER BY MP.").append(COLUMN_PERIOD_REGISTRATION_DATE).append(" DESC, M.")
                .append(COLUMN_LASTNAME).append(" ASC, M.").append(COLUMN_FIRSTNAME).append(" ASC");

        String finalQuery = queryBuilder.toString();
        String[] selectionArgs = selectionArgsList.toArray(new String[0]);

        Log.d(TAG, "Query for getMembershipsStartedInLastThreeMonthsForDisplay (Month: " + registrationMonthFilter + "): " + finalQuery + " with args: " + Arrays.toString(selectionArgs));

        try {
            cursor = db.rawQuery(finalQuery, selectionArgs);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String memberId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEMBER_ID));
                    String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRSTNAME));
                    String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LASTNAME));
                    String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
                    String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
                    int age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE));
                    String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_FILE_PATH));
                    int periodId = cursor.getInt(cursor.getColumnIndexOrThrow("PeriodID"));
                    String regDateStr = cursor.getString(cursor.getColumnIndexOrThrow("RegDate"));
                    String expDateStr = cursor.getString(cursor.getColumnIndexOrThrow("ExpDate"));
                    String receiptNumber = cursor.getString(cursor.getColumnIndexOrThrow("Receipt"));
                    int fkMemberTypeId = cursor.getInt(cursor.getColumnIndexOrThrow("FkMemberTypeID"));
                    String memberTypeName = cursor.getString(cursor.getColumnIndexOrThrow("MemberTypeName"));

                    LocalDate registrationDate = null;
                    if (regDateStr != null) try { registrationDate = LocalDate.parse(regDateStr, SQLITE_DATE_FORMATTER); } catch (Exception ignored) {}
                    LocalDate expirationDate = null;
                    if (expDateStr != null) try { expirationDate = LocalDate.parse(expDateStr, SQLITE_DATE_FORMATTER); } catch (Exception ignored) {}


                    boolean isActive = (expirationDate != null && !expirationDate.isBefore(LocalDate.now()));

                    membersList.add(new MemberDisplayInfo(
                            memberId, firstName, lastName, phoneNumber, gender,
                            age,
                            imagePath,
                            periodId, fkMemberTypeId, memberTypeName != null ? memberTypeName : "N/A",
                            registrationDate, expirationDate, receiptNumber, isActive
                    ));
                } while (cursor.moveToNext());
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error in getMembershipsStartedInLastThreeMonthsForDisplay (Month: " + registrationMonthFilter + ")", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "getMembershipsStartedInLastThreeMonthsForDisplay (Month: " + registrationMonthFilter + ") returned " + membersList.size() + " records.");
        return membersList;
    }

    public MemberDisplayInfo getMemberDisplayInfo(String memberId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        MemberDisplayInfo memberInfo = null;

        String query = "SELECT " +
                "M." + COLUMN_MEMBER_ID + ", " +
                "M." + COLUMN_FIRSTNAME + ", " +
                "M." + COLUMN_LASTNAME + ", " +
                "M." + COLUMN_PHONE_NUMBER + ", " +
                "M." + COLUMN_GENDER + ", " +
                "M." + COLUMN_AGE + ", " +
                "M." + COLUMN_IMAGE_FILE_PATH + ", " +
                "LatestMP." + COLUMN_PERIOD_ID + " AS LatestPeriodID, " +
                "LatestMP." + COLUMN_PERIOD_REGISTRATION_DATE + " AS LatestRegDate, " +
                "LatestMP." + COLUMN_PERIOD_EXPIRATION_DATE + " AS LatestExpDate, " +
                "LatestMP." + COLUMN_PERIOD_RECEIPT_NUMBER + " AS LatestReceipt, " +
                "LatestMP." + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " AS LatestFkMemberTypeID, " +
                "MT." + COLUMN_MT_NAME + " AS LatestMemberTypeName " +
                "FROM " + TABLE_MEMBERS + " M " +
                "LEFT JOIN ( " +
                "    SELECT " + COLUMN_FK_MEMBER_ID_PERIOD + ", MAX(" + COLUMN_PERIOD_EXPIRATION_DATE + ") AS MaxExpDate " +
                "    FROM " + TABLE_MEMBERSHIP_PERIODS +
                "    WHERE " + COLUMN_FK_MEMBER_ID_PERIOD + " = ? " +
                "    GROUP BY " + COLUMN_FK_MEMBER_ID_PERIOD +
                ") AS MaxDatesPerMember ON M." + COLUMN_MEMBER_ID + " = MaxDatesPerMember." + COLUMN_FK_MEMBER_ID_PERIOD + " " +
                "LEFT JOIN " + TABLE_MEMBERSHIP_PERIODS + " LatestMP ON " +
                "    MaxDatesPerMember." + COLUMN_FK_MEMBER_ID_PERIOD + " = LatestMP." + COLUMN_FK_MEMBER_ID_PERIOD + " " +
                "    AND MaxDatesPerMember.MaxExpDate = LatestMP." + COLUMN_PERIOD_EXPIRATION_DATE + " " +
                "    AND LatestMP." + COLUMN_PERIOD_ID + " = ( " +
                "        SELECT MAX(" + COLUMN_PERIOD_ID + ") FROM " + TABLE_MEMBERSHIP_PERIODS +
                "        WHERE " + COLUMN_FK_MEMBER_ID_PERIOD + " = LatestMP." + COLUMN_FK_MEMBER_ID_PERIOD +
                "        AND " + COLUMN_PERIOD_EXPIRATION_DATE + " = LatestMP." + COLUMN_PERIOD_EXPIRATION_DATE +
                "    ) " +
                "LEFT JOIN " + TABLE_MEMBER_TYPES + " MT ON LatestMP." + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " = MT." + COLUMN_MT_ID + " " +
                "WHERE M." + COLUMN_MEMBER_ID + " = ?";

        

        try {
            cursor = db.rawQuery(query, new String[]{memberId, memberId});

            if (cursor != null && cursor.moveToFirst()) {
                String retrievedMemberId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEMBER_ID));
                String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRSTNAME));
                String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LASTNAME));
                String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
                String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
                int age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE));
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_FILE_PATH));

                int latestPeriodId = -1;
                int fkMemberTypeId = -1;
                String memberTypeName = null;
                String latestRegDateStr = null;
                String latestExpDateStr = null;
                String latestReceipt = null;

                int periodIdColIndex = cursor.getColumnIndex("LatestPeriodID");
                if (periodIdColIndex != -1 && !cursor.isNull(periodIdColIndex)) {
                    latestPeriodId = cursor.getInt(periodIdColIndex);
                    fkMemberTypeId = cursor.getInt(cursor.getColumnIndexOrThrow("LatestFkMemberTypeID"));
                    memberTypeName = cursor.getString(cursor.getColumnIndexOrThrow("LatestMemberTypeName"));
                    latestRegDateStr = cursor.getString(cursor.getColumnIndexOrThrow("LatestRegDate"));
                    latestExpDateStr = cursor.getString(cursor.getColumnIndexOrThrow("LatestExpDate"));
                    latestReceipt = cursor.getString(cursor.getColumnIndexOrThrow("LatestReceipt"));
                }

                LocalDate registrationDate = null;
                if (latestRegDateStr != null) {
                    try {
                        registrationDate = LocalDate.parse(latestRegDateStr, SQLITE_DATE_FORMATTER);
                    } catch (Exception ignored) {
                        
                    }
                }

                LocalDate expirationDate = null;
                if (latestExpDateStr != null) {
                    try {
                        expirationDate = LocalDate.parse(latestExpDateStr, SQLITE_DATE_FORMATTER);
                    } catch (Exception ignored) {
                        
                    }
                }

                boolean isActive = false;
                if (expirationDate != null) {
                    isActive = !expirationDate.isBefore(LocalDate.now());
                }

                memberInfo = new MemberDisplayInfo(
                        retrievedMemberId, firstName, lastName, phoneNumber, gender, age, imagePath,
                        latestPeriodId,
                        fkMemberTypeId,
                        memberTypeName != null ? memberTypeName : "N/A",
                        registrationDate,
                        expirationDate,
                        latestReceipt,
                        isActive
                );
            }
        } catch (Exception ignored) {
            
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return memberInfo;
    }


    public boolean isMembershipActive(String memberId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean isActive = false;
        String todayDateStr = LocalDate.now().format(SQLITE_DATE_FORMATTER);

        String query = "SELECT 1 FROM " + TABLE_MEMBERSHIP_PERIODS +
                " WHERE " + COLUMN_FK_MEMBER_ID_PERIOD + " = ? " +
                " AND DATE(" + COLUMN_PERIOD_EXPIRATION_DATE + ") >= DATE(?) " +
                " LIMIT 1";

        try {
            cursor = db.rawQuery(query, new String[]{memberId, todayDateStr});
            if (cursor != null && cursor.moveToFirst()) {
                isActive = true;
            }
        } catch (Exception ignored) {
            
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return isActive;
    }

    public MemberType getMemberTypeById(int typeId) {
        SQLiteDatabase db = this.getReadableDatabase();
        MemberType memberType = null;

        try (Cursor cursor = db.query(
                TABLE_MEMBER_TYPES,
                new String[]{COLUMN_MT_ID, COLUMN_MT_NAME, COLUMN_MT_DURATION_DAYS, COLUMN_MT_IS_TWO_IN_ONE},
                COLUMN_MT_ID + " = ?",
                new String[]{String.valueOf(typeId)},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MT_NAME));
                int durationDays = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MT_DURATION_DAYS));
                boolean isTwoInOne = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MT_IS_TWO_IN_ONE)) == 1;

                memberType = new MemberType(id, name, durationDays, isTwoInOne);
            }
        } catch (Exception ignored) {

        }
        return memberType;
    }

    private boolean isMemberTypeInUse(SQLiteDatabase db, int memberTypeId) {
        String query = "SELECT 1 FROM " + TABLE_MEMBERSHIP_PERIODS +
                " WHERE " + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " = ? LIMIT 1";
        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(memberTypeId)})) {
            if (cursor != null && cursor.moveToFirst()) {
                return true;
            }
        } catch (Exception ignored) {

        }
        return false;
    }

    public boolean deleteMemberType(int typeIdToDelete) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean success = false;

        if (isMemberTypeInUse(db, typeIdToDelete))
        {
            return false;
        }

        db.beginTransaction();
        try {
            int rowsAffected = db.delete(TABLE_MEMBER_TYPES, COLUMN_MT_ID + " = ?", new String[]{String.valueOf(typeIdToDelete)});
            if (rowsAffected > 0) {
                db.setTransactionSuccessful();
                success = true;
                
            } else {
                
                success = false;
            }
        } catch (SQLException e) {
            
            success = false;
        } finally {
            db.endTransaction();
        }
        return success;
    }

    public List<MemberDisplayInfo> getExpiredMembersForDisplay() {
        List<MemberDisplayInfo> expiredMembersList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        String currentDate = LocalDate.now().format(SQLITE_DATE_FORMATTER);

        String query = "SELECT M." + COLUMN_MEMBER_ID + ", M." + COLUMN_FIRSTNAME + ", M." + COLUMN_LASTNAME + ", M." + COLUMN_PHONE_NUMBER +
                ", M." + COLUMN_GENDER + ", M." + COLUMN_AGE + ", M." + COLUMN_IMAGE_FILE_PATH +
                ", LatestMP." + COLUMN_PERIOD_ID + " AS LatestPeriodID" +
                ", LatestMP." + COLUMN_PERIOD_REGISTRATION_DATE + " AS LatestRegDate" +
                ", LatestMP." + COLUMN_PERIOD_EXPIRATION_DATE + " AS LatestExpDate" +
                ", LatestMP." + COLUMN_PERIOD_RECEIPT_NUMBER + " AS LatestReceipt" +
                ", LatestMP." + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " AS LatestFkMemberTypeID" +
                ", MT." + COLUMN_MT_NAME + " AS LatestMemberTypeName " +
                "FROM " + TABLE_MEMBERS + " M " +
                "INNER JOIN ( SELECT " + COLUMN_FK_MEMBER_ID_PERIOD + ", MAX(" + COLUMN_PERIOD_EXPIRATION_DATE + ") AS MaxExpDate " +
                "  FROM " + TABLE_MEMBERSHIP_PERIODS + " GROUP BY " + COLUMN_FK_MEMBER_ID_PERIOD + ") AS MaxDatesPerMember " +
                "  ON M." + COLUMN_MEMBER_ID + " = MaxDatesPerMember." + COLUMN_FK_MEMBER_ID_PERIOD + " " +
                "INNER JOIN " + TABLE_MEMBERSHIP_PERIODS + " LatestMP " +
                "  ON MaxDatesPerMember." + COLUMN_FK_MEMBER_ID_PERIOD + " = LatestMP." + COLUMN_FK_MEMBER_ID_PERIOD + " " +
                "  AND MaxDatesPerMember.MaxExpDate = LatestMP." + COLUMN_PERIOD_EXPIRATION_DATE + " " +
                "INNER JOIN ( SELECT " + COLUMN_FK_MEMBER_ID_PERIOD + ", " + COLUMN_PERIOD_EXPIRATION_DATE +
                ", MAX(" + COLUMN_PERIOD_ID + ") AS MaxPeriodIDForExpDate " +
                "  FROM " + TABLE_MEMBERSHIP_PERIODS + " GROUP BY " + COLUMN_FK_MEMBER_ID_PERIOD + ", " + COLUMN_PERIOD_EXPIRATION_DATE + ") AS LatestPeriodTieBreaker " +
                "  ON LatestMP." + COLUMN_FK_MEMBER_ID_PERIOD + " = LatestPeriodTieBreaker." + COLUMN_FK_MEMBER_ID_PERIOD + " " +
                "  AND LatestMP." + COLUMN_PERIOD_EXPIRATION_DATE + " = LatestPeriodTieBreaker." + COLUMN_PERIOD_EXPIRATION_DATE + " " +
                "  AND LatestMP." + COLUMN_PERIOD_ID + " = LatestPeriodTieBreaker.MaxPeriodIDForExpDate " +
                "LEFT JOIN " + TABLE_MEMBER_TYPES + " MT ON LatestMP." + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " = MT." + COLUMN_MT_ID + " " +
                "WHERE DATE(LatestMP." + COLUMN_PERIOD_EXPIRATION_DATE + ") < DATE('" + currentDate + "') " +
                "AND NOT EXISTS ( SELECT 1 FROM " + TABLE_MEMBERSHIP_PERIODS + " ActiveFutureCheck " +
                "  WHERE ActiveFutureCheck." + COLUMN_FK_MEMBER_ID_PERIOD + " = M." + COLUMN_MEMBER_ID + " " +
                "  AND DATE(ActiveFutureCheck." + COLUMN_PERIOD_EXPIRATION_DATE + ") >= DATE('" + currentDate + "')) " +
                "ORDER BY M." + COLUMN_LASTNAME + " ASC, M." + COLUMN_FIRSTNAME + " ASC";

        

        try {
            cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String memberId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEMBER_ID));
                    String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRSTNAME));
                    String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LASTNAME));
                    String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
                    String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
                    int age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE));
                    String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_FILE_PATH));

                    int latestPeriodId = cursor.getInt(cursor.getColumnIndexOrThrow("LatestPeriodID"));
                    int fkMemberTypeId = cursor.getInt(cursor.getColumnIndexOrThrow("LatestFkMemberTypeID"));
                    String memberTypeName = cursor.getString(cursor.getColumnIndexOrThrow("LatestMemberTypeName"));

                    String latestRegDateStr = cursor.getString(cursor.getColumnIndexOrThrow("LatestRegDate"));
                    String latestExpDateStr = cursor.getString(cursor.getColumnIndexOrThrow("LatestExpDate"));
                    String latestReceipt = cursor.getString(cursor.getColumnIndexOrThrow("LatestReceipt"));


                    LocalDate registrationDate = null;
                    if (latestRegDateStr != null) {
                        try {
                            registrationDate = LocalDate.parse(latestRegDateStr, SQLITE_DATE_FORMATTER);
                        } catch (Exception ignored) {
                            
                        }
                    }
                    LocalDate expirationDate = null;
                    if (latestExpDateStr != null) {
                        try {
                            expirationDate = LocalDate.parse(latestExpDateStr, SQLITE_DATE_FORMATTER);
                        } catch (Exception ignored) {
                            
                        }
                    }

                    boolean isActive = false;

                    MemberDisplayInfo memberInfo = new MemberDisplayInfo(
                            memberId, firstName, lastName, phoneNumber, gender, age, imagePath,
                            latestPeriodId,
                            fkMemberTypeId,
                            memberTypeName,
                            registrationDate,
                            expirationDate,
                            latestReceipt,
                            isActive
                    );
                    expiredMembersList.add(memberInfo);
                    

                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {
            
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        return expiredMembersList;
    }

    public boolean isReceiptNumberExists(String receiptNumber, int periodIdToExclude) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;

        if (receiptNumber == null || receiptNumber.trim().isEmpty()) {
            return false;
        }

        String query = "SELECT " + COLUMN_PERIOD_ID + " FROM " + TABLE_MEMBERSHIP_PERIODS +
                " WHERE " + COLUMN_PERIOD_RECEIPT_NUMBER + " = ?" +
                " AND " + COLUMN_PERIOD_ID + " != ?";

        try {
            cursor = db.rawQuery(query, new String[]{receiptNumber, String.valueOf(periodIdToExclude)});
            if (cursor != null && cursor.getCount() > 0) {
                exists = true;
            }
        } catch (Exception ignored) {
            
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return exists;
    }

    public List<String> getAllMemberImagePaths() {
        List<String> imagePaths = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT DISTINCT " + COLUMN_IMAGE_FILE_PATH + " FROM " + TABLE_MEMBERS +
                " WHERE " + COLUMN_IMAGE_FILE_PATH + " IS NOT NULL AND " +
                COLUMN_IMAGE_FILE_PATH + " != ''";
        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(COLUMN_IMAGE_FILE_PATH);
                if (columnIndex != -1) {
                    do {
                        imagePaths.add(cursor.getString(columnIndex));
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception ignored) {

        }
        return imagePaths;
    }

    public HashMap<String, byte[]> getAllFingerprintTemplates() {
        HashMap<String, byte[]> templates = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_MEMBERS,
                new String[]{COLUMN_MEMBER_ID, COLUMN_FINGERPRINT_TEMPLATE},
                COLUMN_FINGERPRINT_TEMPLATE + " IS NOT NULL",
                null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String memberId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEMBER_ID));
                    byte[] template = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_FINGERPRINT_TEMPLATE));
                    if (memberId != null && template != null) {
                        templates.put(memberId, template);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {

        }
        return templates;
    }

    public synchronized void closeDatabase() {
        super.close();
    }
}
