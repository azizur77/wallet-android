package com.learningmachine.android.app.data.store;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;
import com.learningmachine.android.app.data.model.Certificate;
import com.learningmachine.android.app.data.model.LMDocument;
import com.learningmachine.android.app.data.store.cursor.CertificateCursorWrapper;
import com.learningmachine.android.app.data.webservice.response.AddCertificateResponse;
import com.learningmachine.android.app.data.webservice.response.IssuerResponse;

public class CertificateStore implements DataStore {

    private SQLiteDatabase mDatabase;
    private ImageStore mImageStore;

    public CertificateStore(LMDatabaseHelper databaseHelper, ImageStore imageStore) {
        mDatabase = databaseHelper.getWritableDatabase();
        mImageStore = imageStore;
    }

    public Certificate loadCertificate(String certUuid, String issuerUuid) {

        Certificate certificate = null;
        Cursor cursor = mDatabase.query(
                LMDatabaseHelper.Table.CERTIFICATE,
                null,
                LMDatabaseHelper.Column.Certificate.UUID + " = ? "
                        + " AND " + LMDatabaseHelper.Column.Certificate.ISSUER_UUID + " = ?",
                new String[] { certUuid, issuerUuid },
                null,
                null,
                null);

        if (cursor.moveToFirst()) {
            CertificateCursorWrapper cursorWrapper = new CertificateCursorWrapper(cursor);
            certificate = cursorWrapper.getCertificate();
        }

        cursor.close();

        return certificate;
    }

    public void saveAddCertificateResponse(AddCertificateResponse response) {
        LMDocument document = response.getDocument();
        Certificate certificate = document.getCertificate();

        String certUuid = certificate.getUuid();
        IssuerResponse issuerResponse = certificate.getIssuerResponse();
        String issuerUuid = issuerResponse.getUuid();
        certificate.setIssuerUuid(issuerUuid);

        ContentValues contentValues = createCertificateContentValues(certificate);

        Gson gson = new Gson();
        contentValues.put(LMDatabaseHelper.Column.Certificate.JSON, gson.toJson(response));

        saveCertificateContentValues(contentValues, certUuid, issuerUuid);
    }

    public void saveCertificate(Certificate certificate) {

        String certUuid = certificate.getUuid();
        String issuerUuid = certificate.getIssuerUuid();

        ContentValues contentValues = createCertificateContentValues(certificate);
        saveCertificateContentValues(contentValues, certUuid, issuerUuid);
    }

    private ContentValues createCertificateContentValues(Certificate certificate) {
        ContentValues contentValues = new ContentValues();

        String certUuid = certificate.getUuid();
        String issuerUuid = certificate.getIssuerUuid();

        contentValues.put(LMDatabaseHelper.Column.Certificate.UUID, certUuid);
        contentValues.put(LMDatabaseHelper.Column.Certificate.NAME, certificate.getName());
        contentValues.put(LMDatabaseHelper.Column.Certificate.DESCRIPTION, certificate.getDescription());
        contentValues.put(LMDatabaseHelper.Column.Certificate.ISSUER_UUID, issuerUuid);

        return contentValues;
    }

    private void saveCertificateContentValues(ContentValues contentValues, String certUuid, String issuerUuid) {
        if (loadCertificate(certUuid, issuerUuid) == null) {
            mDatabase.insert(LMDatabaseHelper.Table.CERTIFICATE,
                    null,
                    contentValues);
        } else {
            mDatabase.update(LMDatabaseHelper.Table.CERTIFICATE,
                    contentValues,
                    LMDatabaseHelper.Column.Certificate.UUID + " = ? "
                            + " AND " + LMDatabaseHelper.Column.Certificate.ISSUER_UUID + " = ?",
                    new String[] { certUuid, issuerUuid });
        }
    }

    @Override
    public void reset() {
        mDatabase.delete(LMDatabaseHelper.Table.CERTIFICATE, null, null);
    }
}