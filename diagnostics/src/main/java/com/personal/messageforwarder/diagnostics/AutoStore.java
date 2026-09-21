package com.personal.messageforwarder.diagnostics;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

final class AutoStore extends SQLiteOpenHelper {
    AutoStore(Context c) { this(c, "automatic.db"); }
    AutoStore(Context c, String name) { super(c, name, null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sends(id TEXT PRIMARY KEY,event_key TEXT NOT NULL,destination TEXT NOT NULL,fingerprint TEXT NOT NULL,created INTEGER NOT NULL,source TEXT,rule_names TEXT,parts INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,detail TEXT,UNIQUE(event_key,destination))");
        db.execSQL("CREATE INDEX duplicate_check ON sends(fingerprint,created)");
        db.execSQL("CREATE TABLE results(send_id TEXT,part INTEGER,result INTEGER,PRIMARY KEY(send_id,part))");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int old, int version) { }
    synchronized boolean reserve(String id, String eventKey, String destination, String fingerprint, long now, String source, String rules) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            try (Cursor c = db.rawQuery("SELECT 1 FROM sends WHERE event_key=? AND destination=? LIMIT 1",new String[]{eventKey,destination})) {
                if(c.moveToFirst())return false;
            }
            boolean duplicate;
            try(Cursor c=db.rawQuery("SELECT 1 FROM sends WHERE fingerprint=? AND created>=? AND status!='DUPLICATE' LIMIT 1",new String[]{fingerprint,Long.toString(now-120000)})) {
                duplicate=c.moveToFirst();
            }
            ContentValues v = new ContentValues();
            v.put("id",id); v.put("event_key",eventKey); v.put("destination",destination); v.put("fingerprint",fingerprint);
            v.put("created",now); v.put("source",source); v.put("rule_names",rules); v.put("status",duplicate?"DUPLICATE":"RESERVED"); v.put("detail",duplicate?"같은 본문·대상 2분 내 중복 차단":"발송 준비");
            db.insertOrThrow("sends",null,v);
            db.setTransactionSuccessful(); return !duplicate;
        } finally { db.endTransaction(); }
    }
    synchronized void state(String id, String status, String detail, int parts) {
        ContentValues v = new ContentValues(); v.put("status",status); v.put("detail",detail);
        if (parts >= 0) v.put("parts",parts);
        getWritableDatabase().update("sends",v,"id=?",new String[]{id});
    }
    synchronized void callback(String id, int part, int result) {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            int total; String status;
            try (Cursor c = db.rawQuery("SELECT parts,status FROM sends WHERE id=?",new String[]{id})) {
                if (!c.moveToFirst()) return;
                total=c.getInt(0); status=c.getString(1);
            }
            if (part < 0 || part >= total || status.equals("CANCELLED")) return;
            ContentValues v = new ContentValues(); v.put("send_id",id); v.put("part",part); v.put("result",result);
            db.insertWithOnConflict("results",null,v,SQLiteDatabase.CONFLICT_IGNORE);
            int count=0, failed=0;
            try (Cursor c=db.rawQuery("SELECT result FROM results WHERE send_id=?",new String[]{id})) {
                while(c.moveToNext()){ count++; if(c.getInt(0)!=android.app.Activity.RESULT_OK) failed++; }
            }
            if (failed>0) state(id,"FAILED","일부 또는 전체 발송 실패 (결과 코드 " + result + "). 자동 재시도 없음",-1);
            else if(count==total && !status.equals("FAILED") && !status.equals("UNKNOWN")) state(id,"SENT","모든 SMS 조각 발송 성공. 상대 수신은 별도 확인",-1);
            else if(!status.equals("FAILED") && !status.equals("UNKNOWN")) state(id,"PENDING","발송 결과 " + count + "/" + total + " 확인",-1);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    synchronized String status(String id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT status FROM sends WHERE id=?",new String[]{id})) { return c.moveToFirst()?c.getString(0):""; }
    }
    synchronized String history() {
        StringBuilder out=new StringBuilder();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT created,destination,source,rule_names,status,detail,parts FROM sends ORDER BY created DESC LIMIT 100",null)) {
            while(c.moveToNext()) {
                String state=c.getString(4);
                if((state.equals("PENDING")||state.equals("RESERVED")) && System.currentTimeMillis()-c.getLong(0)>120000) state="결과 미확인 · 자동 재시도 없음";
                else if(state.equals("SENT")) state="발송 성공";
                else if(state.equals("FAILED")) state="발송 실패";
                else if(state.equals("CANCELLED")) state="발송 취소";
                else if(state.equals("DUPLICATE")) state="중복 발송 차단";
                else if(state.equals("UNKNOWN")) state="결과 미확인";
                else state="발송 결과 대기";
                out.append(android.text.format.DateFormat.format("MM-dd HH:mm:ss",c.getLong(0))).append(" · ").append(c.getString(1))
                    .append("\n").append(c.getString(2)).append(" / ").append(c.getString(3)).append("\n").append(state)
                    .append(" · ").append(c.getInt(6)).append("개 SMS 조각\n").append(c.getString(5)).append("\n────────\n");
            }
        }
        return out.length()==0?"자동발송 내역 없음":out.toString();
    }
}

