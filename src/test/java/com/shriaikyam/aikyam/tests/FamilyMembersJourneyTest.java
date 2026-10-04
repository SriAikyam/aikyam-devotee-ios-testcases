package com.shriaikyam.aikyam.tests;

import com.shriaikyam.aikyam.base.BaseTest;
import com.shriaikyam.aikyam.pages.FamilyMembersPage;
import com.shriaikyam.aikyam.pages.ProfilePage;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Self-contained family-member journeys. Each test opens the screen itself and cleans up
 * every member it created, by unique name, even if an assertion failed midway.
 */
public class FamilyMembersJourneyTest extends BaseTest {

    private static final String RELATIONSHIP = "Grandmother";
    private static final String GOTHRAM = "Kashyapa";
    private static final String NAKSHATRAM = "Ashwini";

    /** "QA" + 6 digits = 8 chars, and the rename adds 1 more: well under the 20-char cap. */
    private static String uniqueName() {
        return "QA" + System.currentTimeMillis() % 1_000_000L;
    }

    @Test
    public void TC221_Save_With_Empty_Name_Shows_Error_And_Adds_No_Row() {
        FamilyMembersPage page = openFamilyMembersScreen();
        int start = page.readSettledMemberCount();
        Assert.assertTrue("Starting member count should be readable", start >= 0);

        Assert.assertTrue("Save Members should be tappable with an empty name", page.tapSaveMembers());
        Assert.assertTrue("Validation error should be shown", page.isTextShown("Please enter full name"));
        Assert.assertEquals("No row should be added", start, page.readMemberCountStable());
    }

    @Test
    public void TC222_Full_Name_Over_20_Chars_Is_Capped_And_Adds_No_Row() {
        FamilyMembersPage page = openFamilyMembersScreen();
        int start = page.readSettledMemberCount();
        Assert.assertTrue("Starting member count should be readable", start >= 0);
        String tooLong = "Abcdefghijklmnopqrstuvwxy"; // 25 chars
        try {
            String held = page.typeIntoAddFormFullNameAndRead(tooLong);
            Assert.assertNotNull("Full Name field should be found: " + page.screenTexts(), held);
            // The app drops input past 20 chars (FamilyMemberFormBottomSheet onValueChange).
            Assert.assertTrue("Field must never hold more than 20 chars, held '" + held + "'", held.length() <= 20);
            Assert.assertEquals("Field should keep only the first 20 chars", tooLong.substring(0, 20), held);
            Assert.assertEquals("Nothing was saved, so no row is added", start, page.readMemberCountStable());
            Assert.assertTrue("No row with the full 25-char name", page.isMemberGoneFromList(tooLong));
        } finally {
            page.clearAddFormFullName();
        }
    }

    /** Deletes the test member under whichever name it still has; verifies it is gone. */
    private void cleanup(FamilyMembersPage page, boolean bodyPassed, String... candidates) {
        try {
            page.dismissEditSheet();
            // The add form's Full Name field can still hold the name after a mid-form
            // failure, which would read as a listed row - empty it first.
            page.clearAddFormFullName();
            for (String candidate : candidates) {
                if (candidate != null && !page.isMemberGoneFromList(candidate)) {
                    if (page.openEditSheetForMember(candidate)) {
                        List<String> sheet = page.readEditSheetFields();
                        // Only delete if the sheet really is this member's: never a neighbour.
                        if (sheet != null && candidate.equals(sheet.get(0))) {
                            page.tapDeleteInEditSheet();
                            page.waitEditSheetClosed();
                        } else {
                            page.dismissEditSheet();
                        }
                    }
                }
            }
            for (String candidate : candidates) {
                boolean gone = page.isMemberGoneFromList(candidate);
                if (bodyPassed) {
                    Assert.assertTrue("Cleanup: '" + candidate + "' must be gone", gone);
                } else {
                    System.out.println("[FamilyMembersJourneyTest] cleanup: '" + candidate + "' gone=" + gone);
                }
            }
        } catch (RuntimeException cleanupFailure) {
            System.out.println("[FamilyMembersJourneyTest] cleanup problem: " + cleanupFailure);
        }
    }

    private FamilyMembersPage openFamilyMembersScreen() {
        ProfilePage profilePage = new ProfilePage(driver);
        Assert.assertTrue("Settings hub should open", profilePage.openSettingsHub());
        Assert.assertTrue("Family Profile row should be tappable", profilePage.openFamilyProfile());
        FamilyMembersPage page = new FamilyMembersPage(driver);
        page.waitUntilLoaded();
        return page;
    }
}
