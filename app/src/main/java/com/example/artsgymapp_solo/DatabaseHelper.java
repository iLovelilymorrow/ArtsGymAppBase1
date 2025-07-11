package com.example.artsgymapp_solo;

import static android.content.ContentValues.TAG;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper
{
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


    // --- MemberTypes Table ---
    private static final String TABLE_MEMBER_TYPES = "MemberTypes";
    private static final String COLUMN_MT_ID = "MemberTypeID"; // INTEGER PRIMARY KEY AUTOINCREMENT
    private static final String COLUMN_MT_NAME = "MemberTypeName"; // TEXT UNIQUE NOT NULL
    private static final String COLUMN_MT_DURATION_DAYS = "DurationDays"; // INTEGER NOT NULL
    private static final String COLUMN_MT_IS_TWO_IN_ONE = "isTwoInOne";


    // --- NEW: MembershipPeriods Table ---
    private static final String TABLE_MEMBERSHIP_PERIODS = "MembershipPeriods";
    private static final String COLUMN_PERIOD_ID = "PeriodID"; // INTEGER PRIMARY KEY AUTOINCREMENT
    private static final String COLUMN_FK_MEMBER_ID_PERIOD = "FK_MemberID"; // TEXT (Matches MemberID type)
    private static final String COLUMN_FK_MEMBER_TYPE_ID_PERIOD = "FK_MemberTypeID"; // INTEGER
    private static final String COLUMN_PERIOD_REGISTRATION_DATE = "RegistrationDate"; // TEXT
    private static final String COLUMN_PERIOD_EXPIRATION_DATE = "ExpirationDate"; // TEXT
    private static final String COLUMN_PERIOD_RECEIPT_NUMBER = "ReceiptNumber"; // TEXT

    private static final DateTimeFormatter SQLITE_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String TAG_DB = "DatabaseHelper"; // For logging within DB Helper

    public DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // 1. Create MemberTypes Table (No changes here)
        String createMemberTypesTable = "CREATE TABLE " + TABLE_MEMBER_TYPES + "(" +
                COLUMN_MT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_MT_NAME + " TEXT UNIQUE NOT NULL," +
                COLUMN_MT_DURATION_DAYS + " INTEGER NOT NULL," +
                COLUMN_MT_IS_TWO_IN_ONE + " INTEGER NOT NULL DEFAULT 0" +
                ")";

        sqLiteDatabase.execSQL(createMemberTypesTable);

        addInitialMemberType(sqLiteDatabase, "Regular", 30, 0);
        addInitialMemberType(sqLiteDatabase, "Student", 30, 0);

        // 2. Create Members Table (MODIFIED - columns removed)
        String createMembersTable = "CREATE TABLE " + TABLE_MEMBERS + "(" +
                COLUMN_MEMBER_ID + " TEXT PRIMARY KEY," +
                COLUMN_FIRSTNAME + " TEXT," +
                COLUMN_LASTNAME + " TEXT," +
                COLUMN_PHONE_NUMBER + " TEXT," +
                COLUMN_GENDER + " TEXT," +
                COLUMN_AGE + " INTEGER," +
                COLUMN_IMAGE_FILE_PATH + " TEXT" +
                ")";
        sqLiteDatabase.execSQL(createMembersTable);

        // 3. Create MembershipPeriods Table (NEW)
        String createMembershipPeriodsTable = "CREATE TABLE " + TABLE_MEMBERSHIP_PERIODS + "(" +
                COLUMN_PERIOD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_FK_MEMBER_ID_PERIOD + " TEXT NOT NULL," + // Ensure it's NOT NULL
                COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " INTEGER NOT NULL," + // Ensure it's NOT NULL
                COLUMN_PERIOD_REGISTRATION_DATE + " TEXT NOT NULL," +
                COLUMN_PERIOD_EXPIRATION_DATE + " TEXT NOT NULL," +
                COLUMN_PERIOD_RECEIPT_NUMBER + " TEXT," + // Can be nullable if not always present
                "FOREIGN KEY(" + COLUMN_FK_MEMBER_ID_PERIOD + ") REFERENCES " + TABLE_MEMBERS + "(" + COLUMN_MEMBER_ID + ") ON DELETE CASCADE," + // ON DELETE CASCADE
                "FOREIGN KEY(" + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + ") REFERENCES " + TABLE_MEMBER_TYPES + "(" + COLUMN_MT_ID + ") ON DELETE RESTRICT" + // Or CASCADE/SET NULL as needed
                ")";
        sqLiteDatabase.execSQL(createMembershipPeriodsTable);
    }

    private void addInitialMemberType(SQLiteDatabase db, String name, int durationDays, int isTwoInOne)
    {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MT_NAME, name);
        values.put(COLUMN_MT_DURATION_DAYS, durationDays);
        values.put(COLUMN_MT_IS_TWO_IN_ONE, isTwoInOne);
        db.insert(TABLE_MEMBER_TYPES, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int previousVersion, int newVersion)
    {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_MEMBERS);
        onCreate(sqLiteDatabase);
    }

    public boolean addMemberType(String name, int durationDays, boolean isTwoInOne)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MT_NAME, name);
        values.put(COLUMN_MT_DURATION_DAYS, durationDays);
        values.put(COLUMN_MT_IS_TWO_IN_ONE, isTwoInOne ? 1 : 0);

        long result = db.insert(TABLE_MEMBER_TYPES, null, values);
        db.close();
        return result != -1;
    }

    public List<MemberType> getAllMemberTypes() {
        List<MemberType> memberTypes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            // Make sure you select the ID (COLUMN_MT_ID)
            cursor = db.query(TABLE_MEMBER_TYPES, new String[]{COLUMN_MT_ID, COLUMN_MT_NAME, COLUMN_MT_DURATION_DAYS,COLUMN_MT_IS_TWO_IN_ONE},
                    null, null, null, null, COLUMN_MT_NAME + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Ensure you have getColumnIndexOrThrow for safety
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MT_ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MT_NAME));
                    int durationDays = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MT_DURATION_DAYS));
                    boolean isTwoInOne = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MT_IS_TWO_IN_ONE)) == 1;
                    memberTypes.add(new MemberType(id, name, durationDays, isTwoInOne));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get member types from database", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // No db.close();
        }
        return memberTypes;
    }

    private String insertMemberCore(SQLiteDatabase db, String memberIdToUse, // Allow passing ID for 2-in-1
                                    String firstName, String lastName, String phoneNumber,
                                    String gender, int age, String imageFilePath,
                                    int memberTypeId, LocalDate registrationDate,
                                    LocalDate expirationDate, String receiptNumber) {
        try {
            // Step 1: Insert into Members table
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

            long memberResult = db.insertOrThrow(TABLE_MEMBERS, null, memberValues);

            if (memberResult != -1) {
                // Step 2: Insert into MembershipPeriods table
                // Assuming addInitialMembershipPeriodInternal also doesn't manage transactions
                // and uses the passed 'db' object.
                boolean periodResult = addInitialMembershipPeriodInternal(db, memberIdToUse, memberTypeId,
                        registrationDate, expirationDate, receiptNumber);

                if (periodResult) {
                    Log.d(TAG_DB, "Core insert successful for Member ID: " + memberIdToUse);
                    return memberIdToUse; // Success
                } else {
                    Log.e(TAG_DB, "Core insert: Failed to add initial membership period for Member ID: " + memberIdToUse);
                    // The calling transaction will handle rollback.
                    return null; // Failure
                }
            } else {
                Log.e(TAG_DB, "Core insert: Failed to add member to Members table for Member ID: " + memberIdToUse);
                return null; // Failure
            }
        } catch (SQLException e) {
            Log.e(TAG_DB, "Core insert: SQLException during member insertion for " + memberIdToUse, e);
            return null; // Failure
        }
    }

    public boolean addMember(String firstName, String lastName, String phoneNumber,
                             String gender, int age, String imageFilePath,
                             int memberTypeId, LocalDate registrationDate,
                             LocalDate expirationDate, String receiptNumber) {

        SQLiteDatabase db = this.getWritableDatabase();
        // Generate ID using the same db instance to avoid issues if generateNewMemberId needs it.
        String newMemberId = generateNewMemberId(db);

        if (newMemberId == null) {
            Log.e(TAG_DB, "Failed to generate new Member ID. Cannot add single member.");
            // No db.close() here as it might be handled by SQLiteOpenHelper lifecycle or if an error occurs.
            return false;
        }

        db.beginTransaction(); // Start transaction FOR THIS SINGLE MEMBER
        boolean success = false;
        try {
            String insertedId = insertMemberCore(db, newMemberId, firstName, lastName, phoneNumber,
                    gender, age, imageFilePath, memberTypeId,
                    registrationDate, expirationDate, receiptNumber);

            if (insertedId != null) {
                db.setTransactionSuccessful(); // Commit transaction
                success = true;
                Log.d(TAG_DB, "Single member and period added successfully. Member ID: " + insertedId);
            } else {
                Log.e(TAG_DB, "Single member addition failed during core insert. Member ID attempted: " + newMemberId);
                // Transaction will be rolled back
            }
        } catch (Exception e) { // Broader catch in case insertMemberCore throws something unexpected
            Log.e(TAG_DB, "Exception during single addMember transaction. Member ID attempted: " + newMemberId, e);
            // Transaction will be rolled back
        } finally {
            db.endTransaction(); // End transaction (commit if successful, otherwise rollback)
            // Do not close db here if it's managed by SQLiteOpenHelper
        }
        return success;
    }

    public String addMemberInExternalTransaction(SQLiteDatabase db, String memberIdToUse,
                                                 String firstName, String lastName, String phoneNumber,
                                                 String gender, int age, String imageFilePath,
                                                 int memberTypeId, LocalDate registrationDate,
                                                 LocalDate expirationDate, String receiptNumber) {
        if (db == null || !db.isOpen() || !db.inTransaction()) {
            Log.e(TAG_DB, "addMemberInExternalTransaction called with invalid DB state or not in transaction.");
            return null;
        }
        // Calls the core logic without starting/ending a transaction here.
        return insertMemberCore(db, memberIdToUse, firstName, lastName, phoneNumber, gender, age, imageFilePath,
                memberTypeId, registrationDate, expirationDate, receiptNumber);
    }

    // generateNewMemberId needs to be flexible, if it queries DB, it should use the passed instance.
    public String generateNewMemberId(SQLiteDatabase dbInstance) {
        // If dbInstance is null or not open, you might want to get a new writable one,
        // but for consistency it's better if the caller provides an open instance.
        SQLiteDatabase dbToUse = dbInstance != null && dbInstance.isOpen() ? dbInstance : this.getReadableDatabase();
        boolean closeDbAfter = false;
        if (dbToUse != dbInstance) { // If we had to open a new one
            closeDbAfter = true;
        }


        String newMemberId = "0001"; // Default
        Cursor idCursor = null;
        try {
            // Ensure dbToUse is valid
            if (dbToUse == null || !dbToUse.isOpen()) {
                Log.e(TAG_DB, "Database not available for generating new member ID.");
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
            Log.e(TAG_DB, "Error generating new member ID", e);
            return null; // Indicate failure
        } finally {
            if (idCursor != null) {
                idCursor.close();
            }
            if (closeDbAfter && dbToUse != null && dbToUse.isOpen()) {
                // Only close if we opened it specifically here and it's not the one passed in.
                // dbToUse.close(); // Careful with closing SQLiteOpenHelper managed instances.
                // It's generally better to rely on SQLiteOpenHelper's lifecycle.
                // For generateNewMemberId, if it uses getReadableDatabase(), it's usually fine.
            }
        }
        return newMemberId;
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
            // This should ideally not happen if your UI enforces it.
            // Consider making RegistrationDate NOT NULL in the table if it's always required.
            Log.w(TAG, "Registration date is null for new period for member: " + memberId);
            periodValues.putNull(COLUMN_PERIOD_REGISTRATION_DATE); // Or handle as an error
        }

        if (expirationDate != null) {
            periodValues.put(COLUMN_PERIOD_EXPIRATION_DATE, expirationDate.format(SQLITE_DATE_FORMATTER));
        } else {
            // This should ideally not happen.
            Log.w(TAG, "Expiration date is null for new period for member: " + memberId);
            periodValues.putNull(COLUMN_PERIOD_EXPIRATION_DATE); // Or handle as an error
        }

        if (receiptNumber != null && !receiptNumber.isEmpty()) {
            periodValues.put(COLUMN_PERIOD_RECEIPT_NUMBER, receiptNumber);
        } else {
            periodValues.putNull(COLUMN_PERIOD_RECEIPT_NUMBER);
        }

        try {
            long result = db.insertOrThrow(TABLE_MEMBERSHIP_PERIODS, null, periodValues);
            return result != -1;
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting initial membership period for member: " + memberId, e);
            return false;
        }
    }

    public boolean renewSingleMembership(String memberId, int memberTypeId,
                                         LocalDate registrationDate, LocalDate expirationDate,
                                         String receiptNumber /*, other period details */) {
        SQLiteDatabase db = this.getWritableDatabase();
        // No transaction needed here if it's a single operation,
        // but good practice if multiple DB changes are involved.
        // For consistency, you could use a transaction for single operations too.

        ContentValues periodValues = new ContentValues();
        periodValues.put(COLUMN_FK_MEMBER_ID_PERIOD, memberId);
        periodValues.put(COLUMN_FK_MEMBER_TYPE_ID_PERIOD, memberTypeId);

        if (registrationDate == null || expirationDate == null) {
            Log.e(TAG, "Registration or Expiration date cannot be null when adding a new period.");
            return false; // Or throw IllegalArgumentException
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
            if (result != -1) {
                Log.d(TAG, "Successfully added new membership period for member: " + memberId);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Error adding new membership period for member: " + memberId, e);
        }
        // db.close(); // Manage connection lifecycle appropriately
        return result != -1;
    }

    public boolean renewTwoInOneWithNewPartner(String primaryMemberId,
                                               String newPartnerFirstName, String newPartnerLastName,
                                               String newPartnerPhoneNumber, String newPartnerGender,
                                               int newPartnerAge, String newPartnerImageFilePath,
                                               int memberTypeId, LocalDate registrationDate,
                                               LocalDate expirationDate, String receiptNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        String newPartnerId = null;

        try {
            // 1. Generate ID for the new partner
            newPartnerId = generateNewMemberId(db); // Pass the db instance
            if (newPartnerId == null) {
                Log.e(TAG_DB, "Failed to generate ID for new partner in 2-in-1 renewal.");
                return false; // Exit early
            }

            // 2. Add the new partner to TABLE_MEMBERS (without a period yet)
            // We use a simplified insert here, or you could adapt insertMemberCore
            // to only add to TABLE_MEMBERS if a flag is passed.
            // For now, a direct insert:
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
            long partnerInsertResult = db.insertOrThrow(TABLE_MEMBERS, null, partnerValues);
            if (partnerInsertResult == -1) {
                Log.e(TAG_DB, "Failed to insert new partner in 2-in-1 renewal. Partner ID attempted: " + newPartnerId);
                return false; // Exit early
            }
            Log.d(TAG_DB, "Successfully inserted new partner: " + newPartnerId);


            // 3. Add MembershipPeriod for the primary member
            boolean primaryPeriodAdded = addInitialMembershipPeriodInternal(db, primaryMemberId, memberTypeId,
                    registrationDate, expirationDate, receiptNumber);

            // 4. Add MembershipPeriod for the new partner
            boolean partnerPeriodAdded = false;
            if (primaryPeriodAdded) {
                partnerPeriodAdded = addInitialMembershipPeriodInternal(db, newPartnerId, memberTypeId,
                        registrationDate, expirationDate, receiptNumber);
            }

            if (primaryPeriodAdded && partnerPeriodAdded) {
                db.setTransactionSuccessful();
                success = true;
                Log.d(TAG_DB, "2-in-1 (New Partner) renewal successful for Primary: " + primaryMemberId + ", New Partner: " + newPartnerId);
            } else {
                Log.e(TAG_DB, "2-in-1 (New Partner) renewal failed. Primary Period Added: " + primaryPeriodAdded + ", Partner Period Added: " + partnerPeriodAdded);
            }

        } catch (SQLException e) {
            Log.e(TAG_DB, "SQLException during 2-in-1 (New Partner) renewal.", e);
            success = false;
        } finally {
            db.endTransaction();
            // Do not close db here if helper manages it
        }
        return success;
    }

    public boolean renewTwoInOneWithExistingPartner(String primaryMemberId, String existingPartnerId,
                                                    int memberTypeId, LocalDate registrationDate,
                                                    LocalDate expirationDate, String receiptNumber) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;

        try {
            // Ensure existingPartnerId is valid and different from primaryMemberId (checked in Fragment)
            // Optional: you could re-fetch the existing partner here to ensure they still exist,
            // but getMemberById in the Fragment should suffice if done just before calling this.

            // 1. Add MembershipPeriod for the primary member
            boolean primaryPeriodAdded = addInitialMembershipPeriodInternal(db, primaryMemberId, memberTypeId,
                    registrationDate, expirationDate, receiptNumber);

            // 2. Add MembershipPeriod for the existing partner
            boolean partnerPeriodAdded = false;
            if (primaryPeriodAdded) {
                partnerPeriodAdded = addInitialMembershipPeriodInternal(db, existingPartnerId, memberTypeId,
                        registrationDate, expirationDate, receiptNumber);
            }

            if (primaryPeriodAdded && partnerPeriodAdded) {
                db.setTransactionSuccessful();
                success = true;
                Log.d(TAG_DB, "2-in-1 (Existing Partner) renewal successful for Primary: " + primaryMemberId + ", Existing Partner: " + existingPartnerId);
            } else {
                Log.e(TAG_DB, "2-in-1 (Existing Partner) renewal failed. Primary Period Added: " + primaryPeriodAdded + ", Partner Period Added: " + partnerPeriodAdded);
            }

        } catch (SQLException e) {
            Log.e(TAG_DB, "SQLException during 2-in-1 (Existing Partner) renewal.", e);
            success = false;
        } finally {
            db.endTransaction();
        }
        return success;
    }

    public int getMemberTypeDuration(int memberTypeId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int duration = 0; // Default duration if not found or error

        String query = "SELECT " + COLUMN_MT_DURATION_DAYS + " FROM " + TABLE_MEMBER_TYPES +
                " WHERE " + COLUMN_MT_ID + " = ?";
        try
        {
            cursor = db.rawQuery(query, new String[]{String.valueOf(memberTypeId)});
            if (cursor != null && cursor.moveToFirst())
            {
                duration = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MT_DURATION_DAYS));
            }
            else
            {
                Log.w("DatabaseHelper", "Member type ID " + memberTypeId + " not found to get duration.");
            }
        }
        catch(Exception e)
        {
            Log.e("DatabaseHelper", "Error getting member type duration for ID: " + memberTypeId, e);
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
        return duration;
    }

    public List<MemberDisplayInfo> getActiveMembersForDisplay() { // Or rename existing
        List<MemberDisplayInfo> memberDisplayInfoList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String todayDateStr = LocalDate.now().format(SQLITE_DATE_FORMATTER);

        // This query focuses on getting the LATEST period that is ALSO ACTIVE
        String query = "SELECT " +
                "M." + COLUMN_MEMBER_ID + ", " +
                "M." + COLUMN_FIRSTNAME + ", " +
                "M." + COLUMN_LASTNAME + ", " +
                "M." + COLUMN_PHONE_NUMBER + ", " +
                "M." + COLUMN_GENDER + ", " +
                "M." + COLUMN_AGE + ", " +
                "M." + COLUMN_IMAGE_FILE_PATH + ", " +
                "MP_Active." + COLUMN_PERIOD_ID + " AS ActivePeriodID, " + // Key: ID of the active period
                "MP_Active." + COLUMN_PERIOD_REGISTRATION_DATE + " AS ActiveRegDate, " +
                "MP_Active." + COLUMN_PERIOD_EXPIRATION_DATE + " AS ActiveExpDate, " +
                "MP_Active." + COLUMN_PERIOD_RECEIPT_NUMBER + " AS ActiveReceipt, " +
                "MP_Active." + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " AS ActiveFkMemberTypeID, " +
                "MT_Active." + COLUMN_MT_NAME + " AS ActiveMemberTypeName " +
                "FROM " + TABLE_MEMBERS + " M " +
                // Join with MembershipPeriods to find an ACTIVE period
                "INNER JOIN " + TABLE_MEMBERSHIP_PERIODS + " MP_Active ON M." + COLUMN_MEMBER_ID + " = MP_Active." + COLUMN_FK_MEMBER_ID_PERIOD + " " +
                // Ensure this period is active
                "AND DATE(MP_Active." + COLUMN_PERIOD_EXPIRATION_DATE + ") >= DATE('" + todayDateStr + "') " +
                // Subquery to ensure we only get the MOST RECENT (by expiration date, then by period_id as tie-breaker)
                // among possibly multiple *active* periods for the same member.
                "INNER JOIN (" +
                "    SELECT " + COLUMN_FK_MEMBER_ID_PERIOD + ", MAX(" + COLUMN_PERIOD_EXPIRATION_DATE + ") AS MaxExpDate" +
                "    FROM " + TABLE_MEMBERSHIP_PERIODS +
                "    WHERE DATE(" + COLUMN_PERIOD_EXPIRATION_DATE + ") >= DATE('" + todayDateStr + "')" + // Only consider active periods here too
                "    GROUP BY " + COLUMN_FK_MEMBER_ID_PERIOD +
                ") AS MaxActivePeriods ON MP_Active." + COLUMN_FK_MEMBER_ID_PERIOD + " = MaxActivePeriods." + COLUMN_FK_MEMBER_ID_PERIOD +
                "    AND MP_Active." + COLUMN_PERIOD_EXPIRATION_DATE + " = MaxActivePeriods.MaxExpDate " +
                // Tie-breaker: If multiple active periods expire on the same day, pick the one with the highest Period_ID
                "INNER JOIN (" +
                "    SELECT " + COLUMN_FK_MEMBER_ID_PERIOD + ", " + COLUMN_PERIOD_EXPIRATION_DATE + ", MAX(" + COLUMN_PERIOD_ID + ") AS MaxPeriodIDForMaxDate" +
                "    FROM " + TABLE_MEMBERSHIP_PERIODS +
                "    WHERE DATE(" + COLUMN_PERIOD_EXPIRATION_DATE + ") >= DATE('" + todayDateStr + "')" +
                "    GROUP BY " + COLUMN_FK_MEMBER_ID_PERIOD + ", " + COLUMN_PERIOD_EXPIRATION_DATE +
                ") AS ActiveTieBreaker ON MP_Active." + COLUMN_FK_MEMBER_ID_PERIOD + " = ActiveTieBreaker." + COLUMN_FK_MEMBER_ID_PERIOD +
                "    AND MP_Active." + COLUMN_PERIOD_EXPIRATION_DATE + " = ActiveTieBreaker." + COLUMN_PERIOD_EXPIRATION_DATE +
                "    AND MP_Active." + COLUMN_PERIOD_ID + " = ActiveTieBreaker.MaxPeriodIDForMaxDate " +
                "LEFT JOIN " + TABLE_MEMBER_TYPES + " MT_Active ON MP_Active." + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " = MT_Active." + COLUMN_MT_ID + " " +
                "ORDER BY M." + COLUMN_MEMBER_ID + " DESC";

        Log.d(TAG, "getActiveMembersForDisplay Query: " + query);

        try {
            cursor = db.rawQuery(query, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        // ++ IMPORTANT: Use the aliased column names from the query ++
                        String memberId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEMBER_ID));
                        String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRSTNAME));
                        String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LASTNAME));
                        String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
                        String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
                        int age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE));
                        String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_FILE_PATH));

                        // These will now correspond to the *active* period identified by the query
                        int activePeriodId = cursor.getInt(cursor.getColumnIndexOrThrow("ActivePeriodID")); // Not -1 if member is in result
                        String regDateStr = cursor.getString(cursor.getColumnIndexOrThrow("ActiveRegDate"));
                        String expDateStr = cursor.getString(cursor.getColumnIndexOrThrow("ActiveExpDate"));
                        String receiptNumber = cursor.getString(cursor.getColumnIndexOrThrow("ActiveReceipt"));
                        int fkMemberTypeId = cursor.getInt(cursor.getColumnIndexOrThrow("ActiveFkMemberTypeID"));
                        String memberTypeName = cursor.getString(cursor.getColumnIndexOrThrow("ActiveMemberTypeName"));

                        LocalDate registrationDate = null;
                        if (regDateStr != null) try { registrationDate = LocalDate.parse(regDateStr, SQLITE_DATE_FORMATTER); } catch (Exception e) { Log.e(TAG, "Parse regDate error", e); }

                        LocalDate expirationDate = null;
                        if (expDateStr != null) try { expirationDate = LocalDate.parse(expDateStr, SQLITE_DATE_FORMATTER); } catch (Exception e) { Log.e(TAG, "Parse expDate error", e); }

                        // 'isActive' will always be true here because the query filters for it.
                        // But good practice to derive it if needed or for consistency.
                        boolean isActive = (expirationDate != null && !expirationDate.isBefore(LocalDate.now()));

                        MemberDisplayInfo displayInfo = new MemberDisplayInfo(
                                memberId, firstName, lastName, phoneNumber, gender, age, imagePath,
                                activePeriodId, // This is the ID of their latest *active* period
                                fkMemberTypeId,
                                memberTypeName != null ? memberTypeName : "N/A", // Ensure not null
                                registrationDate,
                                expirationDate,
                                receiptNumber,
                                isActive // Should always be true due to query filter
                        );
                        memberDisplayInfoList.add(displayInfo);
                    } while (cursor.moveToNext());
                } else {
                    Log.d(TAG, "No *active* members found for display.");
                }
            } else {
                Log.e(TAG, "Cursor is null after querying for active members display.");
            }
        } catch (Exception e) { // Catch generic Exception for safety, then more specific ones
            Log.e(TAG, "Error in getActiveMembersForDisplay: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "Returning " + memberDisplayInfoList.size() + " active member display infos.");
        return memberDisplayInfoList;
    }

    public boolean updateMemberAndPeriod(Member member, MembershipPeriod period) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        boolean success = false;
        try {
            // 1. Update Member Table
            ContentValues memberValues = new ContentValues();
            memberValues.put(COLUMN_FIRSTNAME, member.getFirstName());
            memberValues.put(COLUMN_LASTNAME, member.getLastName());
            memberValues.put(COLUMN_PHONE_NUMBER, member.getPhoneNumber());
            memberValues.put(COLUMN_GENDER, member.getGender());
            memberValues.put(COLUMN_AGE, member.getAge());
            memberValues.put(COLUMN_IMAGE_FILE_PATH, member.getImageFilePath());

            int memberRowsAffected = db.update(TABLE_MEMBERS, memberValues,
                    COLUMN_MEMBER_ID + " = ?", new String[]{member.getMemberID()});

            // 2. Update Membership Period Table
            ContentValues periodValues = new ContentValues();
            periodValues.put(COLUMN_FK_MEMBER_TYPE_ID_PERIOD, period.getFkMemberTypeId());
            // periodValues.put(COLUMN_MEMBER_TYPE_NAME_PERIOD, period.getMemberTypeName()); // If you store type name in periods table
            periodValues.put(COLUMN_PERIOD_EXPIRATION_DATE, period.getExpirationDate() != null ? period.getExpirationDate().toString() : null);
            periodValues.put(COLUMN_PERIOD_RECEIPT_NUMBER, period.getReceiptNumber());
            // COLUMN_PERIOD_REGISTRATION_DATE is typically not updated for an existing period
            // unless specifically intended.

            int periodRowsAffected = db.update(TABLE_MEMBERSHIP_PERIODS, periodValues,
                    COLUMN_PERIOD_ID + " = ?", new String[]{String.valueOf(period.getPeriodId())});

            if (memberRowsAffected > 0 && periodRowsAffected > 0) {
                db.setTransactionSuccessful();
                success = true;
                Log.d(TAG, "Successfully updated member and period. Member ID: " + member.getMemberID() + ", Period ID: " + period.getPeriodId());
            } else {
                if (memberRowsAffected <= 0) Log.w(TAG, "Failed to update member or member not found. ID: " + member.getMemberID());
                if (periodRowsAffected <= 0) Log.w(TAG, "Failed to update period or period not found. ID: " + period.getPeriodId());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating member and period in transaction", e);
        } finally {
            db.endTransaction();
            // db.close(); // Manage connection
        }
        return success;
    }

    public Member getMemberById(String memberIdString) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Member member = null;

        // Query ONLY the Members table
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

                // Use the correct Member constructor
                member = new Member(retrievedMemberID, firstName, lastName, phoneNumber, gender, age, imagePath);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get member by ID: " + memberIdString, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // db.close(); // Manage connection
        }
        return member;
    }

    public boolean deleteMember(String memberId)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        String imagePathToDelete = null;
        boolean success = false;

        // --- 1. Get the image file path ---
        Cursor cursor = null;
        try {
            cursor = db.query(
                    TABLE_MEMBERS,
                    new String[]{COLUMN_IMAGE_FILE_PATH},
                    COLUMN_MEMBER_ID + " = ?",
                    new String[]{memberId},
                    null, null, null
            );
            if (cursor != null && cursor.moveToFirst()) {
                int imagePathColumnIndex = cursor.getColumnIndex(COLUMN_IMAGE_FILE_PATH);
                if (imagePathColumnIndex != -1) {
                    imagePathToDelete = cursor.getString(imagePathColumnIndex);
                } else {
                    Log.w(TAG, "Image file path column not found for member: " + memberId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying for image path before deleting member: " + memberId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        db.beginTransaction(); // Start transaction
        try {
            // --- 2. Delete associated membership periods ---
            int periodsDeleted = db.delete(
                    TABLE_MEMBERSHIP_PERIODS,
                    COLUMN_FK_MEMBER_ID_PERIOD + " = ?",
                    new String[]{memberId}
            );
            Log.d(TAG, "Deleted " + periodsDeleted + " periods for member ID: " + memberId);

            // --- 3. Delete the member from the members table ---
            int memberRowsDeleted = db.delete(
                    TABLE_MEMBERS,
                    COLUMN_MEMBER_ID + " = ?",
                    new String[]{memberId}
            );

            if (memberRowsDeleted > 0) {
                db.setTransactionSuccessful();
                success = true;
                Log.d(TAG, "Successfully deleted member with ID: " + memberId + " and their periods.");
            } else {
                Log.w(TAG, "Member with ID: " + memberId + " not found or not deleted.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error deleting member with ID: " + memberId + " or their periods.", e);
            success = false;
        } finally {
            db.endTransaction(); // End transaction (commit or rollback)
            // db.close(); // Manage connection
        }

        // --- 4. Delete the image file if DB deletion was successful ---
        if (success && imagePathToDelete != null && !imagePathToDelete.isEmpty()) {
            try {
                File imageFile = new File(imagePathToDelete);
                if (imageFile.exists()) {
                    if (imageFile.delete()) {
                        Log.d(TAG, "Successfully deleted image file: " + imagePathToDelete);
                    } else {
                        Log.w(TAG, "Failed to delete image file: " + imagePathToDelete);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error trying to delete image file: " + imagePathToDelete, e);
            }
        }
        return success;
    }

    public MembershipPeriod getMembershipPeriodById(int periodId) {
        SQLiteDatabase db = this.getReadableDatabase();
        MembershipPeriod period = null;
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_MEMBERSHIP_PERIODS, null, COLUMN_PERIOD_ID + "=?",
                    new String[]{String.valueOf(periodId)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                period = new MembershipPeriod();
                period.setPeriodId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PERIOD_ID)));
                period.setFkMemberId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FK_MEMBER_ID_PERIOD)));
                period.setFkMemberTypeId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FK_MEMBER_TYPE_ID_PERIOD)));
                // You might need to join with member_types table here to get the name,
                // or fetch it separately as done in loadMemberData for simplicity now.
                // For now, let's assume memberTypeName might be null here or fetched later.
                // period.setMemberTypeName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEMBER_TYPE_NAME_PERIOD))); // If you have such a column

                String regDateStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PERIOD_REGISTRATION_DATE));
                if (regDateStr != null) period.setRegistrationDate(LocalDate.parse(regDateStr));

                String expDateStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PERIOD_EXPIRATION_DATE));
                if (expDateStr != null) period.setExpirationDate(LocalDate.parse(expDateStr));

                period.setReceiptNumber(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PERIOD_RECEIPT_NUMBER)));
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting membership period by ID: " + periodId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // db.close(); // Don't close db here if it's managed by a singleton or opened once per helper instance
        }
        return period;
    }

    public List<String> getMemberIdsByReceiptNumber(String receiptNumber) {
        List<String> memberIds = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        // Search in TABLE_MEMBERSHIP_PERIODS for the receipt number
        String query = "SELECT DISTINCT " + COLUMN_FK_MEMBER_ID_PERIOD +
                " FROM " + TABLE_MEMBERSHIP_PERIODS +
                " WHERE " + COLUMN_PERIOD_RECEIPT_NUMBER + " LIKE ?";

        // Using "LIKE" with wildcards allows for partial matches if desired,
        // or use "=" for exact matches:
        // String query = "SELECT DISTINCT " + COLUMN_FK_MEMBER_ID_PERIOD +
        //                " FROM " + TABLE_MEMBERSHIP_PERIODS +
        //                " WHERE " + COLUMN_PERIOD_RECEIPT_NUMBER + " = ?";

        Log.d(TAG, "getMemberIdsByReceiptNumber query for: " + receiptNumber);

        try {
            cursor = db.rawQuery(query, new String[]{"%" + receiptNumber + "%"}); // For LIKE '%%'
            // If using "=", use: new String[]{receiptNumber}

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    memberIds.add(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FK_MEMBER_ID_PERIOD)));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching member IDs by receipt number: " + receiptNumber, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Do not close db here
        }
        Log.d(TAG, "Found " + memberIds.size() + " member IDs for receipt query: " + receiptNumber);
        return memberIds;
    }

    public MemberType getMemberTypeById(int typeId) {
        SQLiteDatabase db = this.getReadableDatabase();
        MemberType memberType = null;
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_MEMBER_TYPES,   // Table to query
                    new String[]{COLUMN_MT_ID, COLUMN_MT_NAME, COLUMN_MT_DURATION_DAYS,COLUMN_MT_IS_TWO_IN_ONE}, // Columns to return
                    COLUMN_MT_ID + " = ?", // WHERE clause
                    new String[]{String.valueOf(typeId)}, // Arguments for WHERE clause
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MT_NAME));
                int durationDays = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MT_DURATION_DAYS));
                boolean isTwoInOne = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MT_IS_TWO_IN_ONE)) == 1;

                // Assuming your MemberType constructor is (int id, String name, int durationDays)
                memberType = new MemberType(id, name, durationDays, isTwoInOne);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error getting member type by ID: " + typeId + ". Column not found?", e);
        } catch (Exception e) {
            Log.e(TAG, "Error getting member type by ID: " + typeId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // db.close(); // Manage connection appropriately
        }
        return memberType;
    }

    private boolean isMemberTypeInUse(SQLiteDatabase db, int memberTypeId) {
        String query = "SELECT 1 FROM " + TABLE_MEMBERSHIP_PERIODS +
                " WHERE " + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " = ? LIMIT 1";
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, new String[]{String.valueOf(memberTypeId)});
            if (cursor != null && cursor.moveToFirst()) {
                return true; // Found at least one period using this type
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if member type " + memberTypeId + " is in use.", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    public boolean deleteMemberType(int typeIdToDelete) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean success = false;

        // 1. Check if the MemberType is being used in TABLE_MEMBERSHIP_PERIODS
        if (isMemberTypeInUse(db, typeIdToDelete)) {
            Log.w(TAG, "Attempted to delete MemberType ID: " + typeIdToDelete +
                    ", but it is currently in use by one or more membership periods.");
            return false; // Indicate failure because it's in use
        }

        // 2. If not in use, proceed with deletion
        db.beginTransaction();
        try {
            int rowsAffected = db.delete(TABLE_MEMBER_TYPES, COLUMN_MT_ID + " = ?", new String[]{String.valueOf(typeIdToDelete)});
            if (rowsAffected > 0) {
                db.setTransactionSuccessful();
                success = true;
                Log.d(TAG, "Successfully deleted member type with ID: " + typeIdToDelete);
            } else
            {
                Log.w(TAG, "Could not delete member type with ID: " + typeIdToDelete + ". It might not exist.");
                success = false;
            }
        } catch (SQLException e) {
            Log.e(TAG, "SQL Error deleting member type with ID: " + typeIdToDelete, e);
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

        String currentDate = LocalDate.now().format(SQLITE_DATE_FORMATTER); // Correctly using your static formatter

        String query = "SELECT M." + COLUMN_MEMBER_ID + ", M." + COLUMN_FIRSTNAME + ", M." + COLUMN_LASTNAME + ", M." + COLUMN_PHONE_NUMBER +
                ", M." + COLUMN_GENDER + ", M." + COLUMN_AGE + ", M." + COLUMN_IMAGE_FILE_PATH +
                ", LatestMP." + COLUMN_PERIOD_ID + " AS LatestPeriodID" +
                ", LatestMP." + COLUMN_PERIOD_REGISTRATION_DATE + " AS LatestRegDate" +
                ", LatestMP." + COLUMN_PERIOD_EXPIRATION_DATE + " AS LatestExpDate" +
                ", LatestMP." + COLUMN_PERIOD_RECEIPT_NUMBER + " AS LatestReceipt" +
                ", LatestMP." + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " AS LatestFkMemberTypeID" + // Added this alias
                ", MT." + COLUMN_MT_NAME + " AS LatestMemberTypeName " + // Corrected alias to match what MemberDisplayInfo expects for type name
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
                "LEFT JOIN " + TABLE_MEMBER_TYPES + " MT ON LatestMP." + COLUMN_FK_MEMBER_TYPE_ID_PERIOD + " = MT." + COLUMN_MT_ID + " " + // Corrected table alias to MT
                "WHERE DATE(LatestMP." + COLUMN_PERIOD_EXPIRATION_DATE + ") < DATE('" + currentDate + "') " +
                "AND NOT EXISTS ( SELECT 1 FROM " + TABLE_MEMBERSHIP_PERIODS + " ActiveFutureCheck " +
                "  WHERE ActiveFutureCheck." + COLUMN_FK_MEMBER_ID_PERIOD + " = M." + COLUMN_MEMBER_ID + " " +
                "  AND DATE(ActiveFutureCheck." + COLUMN_PERIOD_EXPIRATION_DATE + ") >= DATE('" + currentDate + "')) " +
                "ORDER BY M." + COLUMN_LASTNAME + " ASC, M." + COLUMN_FIRSTNAME + " ASC";

        Log.d("DatabaseHelper_Expired", "Executing query for expired members: " + query);

        try {
            cursor = db.rawQuery(query, null);
            Log.d("DatabaseHelper_Expired", "Query returned " + (cursor == null ? "null cursor" : cursor.getCount() + " rows"));

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String memberId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MEMBER_ID));
                    String firstName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FIRSTNAME)); // Corrected constant
                    String lastName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LASTNAME));   // Corrected constant
                    String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER));
                    String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
                    int age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE));
                    String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_FILE_PATH));

                    int latestPeriodId = cursor.getInt(cursor.getColumnIndexOrThrow("LatestPeriodID"));
                    int fkMemberTypeId = cursor.getInt(cursor.getColumnIndexOrThrow("LatestFkMemberTypeID")); // Get FkMemberTypeID
                    String memberTypeName = cursor.getString(cursor.getColumnIndexOrThrow("LatestMemberTypeName")); // Get MemberTypeName

                    String latestRegDateStr = cursor.getString(cursor.getColumnIndexOrThrow("LatestRegDate"));
                    String latestExpDateStr = cursor.getString(cursor.getColumnIndexOrThrow("LatestExpDate"));
                    String latestReceipt = cursor.getString(cursor.getColumnIndexOrThrow("LatestReceipt"));


                    LocalDate registrationDate = null;
                    if (latestRegDateStr != null) {
                        try {
                            registrationDate = LocalDate.parse(latestRegDateStr, SQLITE_DATE_FORMATTER);
                        } catch (Exception e) { Log.e("DatabaseHelper_Expired", "Error parsing reg date: " + latestRegDateStr, e); }
                    }
                    LocalDate expirationDate = null;
                    if (latestExpDateStr != null) {
                        try {
                            expirationDate = LocalDate.parse(latestExpDateStr, SQLITE_DATE_FORMATTER);
                        } catch (Exception e) { Log.e("DatabaseHelper_Expired", "Error parsing exp date: " + latestExpDateStr, e); }
                    }

                    // For expired members, isActive will be false.
                    // The query condition DATE(LatestMP.ExpirationDate) < DATE(currentDate) ensures this.
                    boolean isActive = false;

                    MemberDisplayInfo memberInfo = new MemberDisplayInfo(
                            memberId, firstName, lastName, phoneNumber, gender, age, imagePath,
                            latestPeriodId,
                            fkMemberTypeId,         // Now correctly passing fkMemberTypeId
                            memberTypeName,         // Now correctly passing memberTypeName
                            registrationDate,
                            expirationDate,
                            latestReceipt,
                            isActive                // Passing false for expired members
                    );
                    expiredMembersList.add(memberInfo);
                    Log.d("DatabaseHelper_Expired", "Added expired member: " + firstName + " " + lastName + ", Exp: " + latestExpDateStr);

                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper_Expired", "Error getting expired members: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // db.close(); // Only if you are not managing db instance elsewhere
        }
        Log.d("DatabaseHelper_Expired", "getExpiredMembersForDisplay returning " + expiredMembersList.size() + " members.");
        return expiredMembersList;
    }

    public boolean isReceiptNumberExists(String receiptNumber, int periodIdToExclude) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;

        if (receiptNumber == null || receiptNumber.trim().isEmpty()) {
            return false; // Empty or null receipt numbers are not considered "existing" in this check.
            // Or handle as an error if receipt numbers are mandatory.
        }

        // Query to check if the receipt number exists in any period,
        // optionally excluding a specific period_id (useful when editing an existing period).
        String query = "SELECT " + COLUMN_PERIOD_ID + " FROM " + TABLE_MEMBERSHIP_PERIODS +
                " WHERE " + COLUMN_PERIOD_RECEIPT_NUMBER + " = ?" +
                " AND " + COLUMN_PERIOD_ID + " != ?"; // Exclude the current period being edited

        try {
            cursor = db.rawQuery(query, new String[]{receiptNumber, String.valueOf(periodIdToExclude)});
            if (cursor != null && cursor.getCount() > 0) {
                exists = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if receipt number '" + receiptNumber + "' exists: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (exists) {
            Log.d(TAG, "Receipt number '" + receiptNumber + "' already exists (excluding period ID " + periodIdToExclude + ").");
        }
        return exists;
    }

    public List<String> getAllMemberImagePaths() {
        List<String> imagePaths = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        // Assuming your image path column is COLUMN_IMAGE_FILE_PATH in TABLE_MEMBERS
        String query = "SELECT DISTINCT " + COLUMN_IMAGE_FILE_PATH + " FROM " + TABLE_MEMBERS +
                " WHERE " + COLUMN_IMAGE_FILE_PATH + " IS NOT NULL AND " +
                COLUMN_IMAGE_FILE_PATH + " != ''"; // Ensure it's not an empty string either
        try {
            cursor = db.rawQuery(query, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(COLUMN_IMAGE_FILE_PATH);
                if (columnIndex != -1) { // Check if column exists
                    do {
                        imagePaths.add(cursor.getString(columnIndex));
                    } while (cursor.moveToNext());
                } else {
                    Log.e("DatabaseHelper", "Column " + COLUMN_IMAGE_FILE_PATH + " not found in " + TABLE_MEMBERS);
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error fetching all image paths", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Don't close db if it's managed by the SQLiteOpenHelper instance
            // and will be closed when the helper is closed.
        }
        Log.d("DatabaseHelper", "Found " + imagePaths.size() + " unique image paths for export.");
        return imagePaths;
    }

    public synchronized void closeDatabase() {
        super.close(); // SQLiteOpenHelper's close method handles closing the database
        Log.d(TAG, "Database connection closed via closeDatabase().");
    }
}