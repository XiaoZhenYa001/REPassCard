package com.example.passcard.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ImportReceiptFactoryTest {
    @Test fun allSelectedRecordsAlreadyPresentAreReportedAsSuccessWithoutNewWrites() {
        val receipt = buildImportDoneReceipt(0, 3, 0, 3, 250, AppLanguage.CHINESE)
        assertEquals(ImportReceiptLevel.SUCCESS, receipt.level)
        assertEquals("所选记录已全部在保险库中", receipt.title)
        assertEquals("3", receipt.primaryValue)
        assertEquals("已存在", receipt.primaryLabel)
        assertEquals("0", receipt.secondaryValue)
        assertEquals(ImportReceiptActionType.SHOW_ISSUES, receipt.primaryAction)
        assertEquals(ImportReceiptActionType.CLOSE_PREVIEW, receipt.secondaryAction)
        assertFalse(receipt.feedItems.any { it.tone == ImportReceiptFeedTone.ERROR })
    }

    @Test fun alreadyPresentSelectionDoesNotHideInvalidRowsFromTheFile() {
        val receipt = buildImportDoneReceipt(0, 2, 1, 2, 250, AppLanguage.ENGLISH)
        assertEquals(ImportReceiptLevel.WARNING, receipt.level)
        assertEquals("All selected records are already in the vault", receipt.title)
        assertEquals("2", receipt.primaryValue)
        assertEquals("1", receipt.secondaryValue)
        assertEquals(2, receipt.feedItems.size)
        assertEquals(ImportReceiptActionType.SHOW_ISSUES, receipt.primaryAction)
    }

    @Test fun zeroWritesWithoutIdenticalRecordsDoesNotClaimSuccess() {
        val receipt = buildImportDoneReceipt(0, 0, 1, 1, 250, AppLanguage.CHINESE)
        assertEquals(ImportReceiptLevel.ERROR, receipt.level)
        assertEquals(ImportReceiptActionType.PICK_FILE, receipt.primaryAction)
    }

    @Test fun completedNewWritesStillReportTheirActualCount() {
        val receipt = buildImportDoneReceipt(2, 0, 0, 2, 250, AppLanguage.ENGLISH)
        assertEquals(ImportReceiptLevel.SUCCESS, receipt.level)
        assertEquals("2", receipt.primaryValue)
        assertEquals("Imported", receipt.primaryLabel)
    }
}
