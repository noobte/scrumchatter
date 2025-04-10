/*
 * Copyright 2013-2017 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.scrumchatter.member.list;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ca.rmen.android.scrumchatter.util.Log;
import ca.rmen.android.scrumchatter.Constants;
import ca.rmen.android.scrumchatter.R;
import ca.rmen.android.scrumchatter.dialog.DialogFragmentFactory;
import ca.rmen.android.scrumchatter.dialog.InputDialogFragment.InputValidator;
import ca.rmen.android.scrumchatter.provider.MemberColumns;
import ca.rmen.android.scrumchatter.team.Teams;
import io.reactivex.schedulers.Schedulers;

/**
 * Provides both UI and DB logic regarding the management of members: creating, and deleting members for now.
 */
public class Members {
    private static final String TAG = Constants.TAG + "/" + Members.class.getSimpleName();
    public static final String EXTRA_MEMBER_ID = "member_id";
    private static final String EXTRA_MEMBER_NAME = "member_name";
    private final FragmentActivity mActivity;

    public static class Member {
        private final long id;
        private final String name;

        Member(long memberId, String memberName) {
            this.id = memberId;
            this.name = memberName;
        }
    }

    public Members(FragmentActivity activity) {
        mActivity = activity;
    }

    /**
     * Show a dialog with a text input for the new member name. Validate that the member doesn't already exist in the given team. Upon pressing "OK", create the
     * member.
     * 
     * @param teamId the id of the team in which the member should be added
     */
    void promptCreateMember(final int teamId) {
        Log.v(TAG, "createMember, teamId = " + teamId);
        Bundle extras = new Bundle(1);
        extras.putLong(Teams.EXTRA_TEAM_ID, teamId);
        DialogFragmentFactory.showInputDialog(mActivity, mActivity.getString(R.string.action_new_member), mActivity.getString(R.string.hint_new_member), null,
                MemberNameValidator.class, R.id.fab_new_member, extras);
    }

    /**
     * Adds a member with the given name to the given team, in the DB.
     */
    public void createMember(final long teamId, final String memberName) {
        Log.v(TAG, "createMember, teamId=" + teamId + ", memberName=" + memberName);

        // Ignore an empty name.
        if (!TextUtils.isEmpty(memberName)) {
            // Create the new member in a background thread.
            Schedulers.io().scheduleDirect(() -> {
                ContentValues values = new ContentValues(2);
                values.put(MemberColumns.NAME, memberName);
                values.put(MemberColumns.TEAM_ID, teamId);
                values.put(MemberColumns.DELETED, 0);
                mActivity.getContentResolver().insert(MemberColumns.CONTENT_URI, values);
            });
        }
    }

    /**
     * Show a dialog with a text input for the member name. Validate that the member doesn't already exist in the given team. Upon pressing "OK", update the
     * name of the member.
     *
     * @param teamId the id of the member's team
     * @param member the team member to rename
     */
    void promptRenameMember(final long teamId, final Member member) {
        Log.v(TAG, "promptRenameMember, teamId = " + teamId + ", member = " + member);
        Bundle extras = new Bundle(1);
        extras.putLong(Teams.EXTRA_TEAM_ID, teamId);
        extras.putLong(EXTRA_MEMBER_ID, member.id);
        extras.putString(EXTRA_MEMBER_NAME, member.name);
        DialogFragmentFactory.showInputDialog(mActivity, mActivity.getString(R.string.action_rename_member), mActivity.getString(R.string.hint_new_member), member.name,
                MemberNameValidator.class, R.id.action_rename_member, extras);
    }

    /**
     * Renames a member
     */
    public void renameMember(final long memberId, final String memberName) {
        Log.v(TAG, "rename member " + memberId + " to " + memberName);

        // Rename the member in a background thread
        Schedulers.io().scheduleDirect(() -> {
            Uri uri = Uri.withAppendedPath(MemberColumns.CONTENT_URI, String.valueOf(memberId));
            ContentValues values = new ContentValues(1);
            values.put(MemberColumns.NAME, memberName);
            mActivity.getContentResolver().update(uri, values, null, null);
        });
    }

    /**
     * Shows a confirmation dialog to the user, to delete a member.
     */
    void confirmDeleteMember(final Member member) {
        Log.v(TAG, "confirm delete member " + member);
        // Let's ask him if he's sure.
        Bundle extras = new Bundle(1);
        extras.putLong(EXTRA_MEMBER_ID, member.id);
        DialogFragmentFactory.showConfirmDialog(mActivity, mActivity.getString(R.string.action_delete_member),
                mActivity.getString(R.string.dialog_message_delete_member_confirm, member.name), R.id.action_delete_member, extras);
    }



    /**
     * Marks a member as deleted.
     */
    public void deleteMember(final long memberId) {
        Log.v(TAG, "delete member " + memberId);


        //get existing user's name
        String name = "";
        Cursor existingCursor = mActivity.getContentResolver().query(MemberColumns.CONTENT_URI, new String[] { MemberColumns.NAME },
                MemberColumns._ID + "=? AND " + MemberColumns.DELETED + "=0", new String[] { String.valueOf(memberId) }, null);

        if (existingCursor != null) {
            if (existingCursor.moveToFirst()) {
                name = existingCursor.getString(0) + " ";
                existingCursor.close();
            }
        }

        name += "(deleted: " + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()) + ")";
        scheduleUserDelete(memberId, name);
    }

    private void scheduleUserDelete(final long memberId, final String updatedName){
        // Delete the member in a background thread
        Schedulers.io().scheduleDirect(() -> {
            Uri uri = Uri.withAppendedPath(MemberColumns.CONTENT_URI, String.valueOf(memberId));
            ContentValues values = new ContentValues(1);
            values.put(MemberColumns.DELETED, 1);
            values.put(MemberColumns.NAME, updatedName);
            mActivity.getContentResolver().update(uri, values, null, null);
        });
    }


    /**
     * Returns an error if the user entered the name of another member in the given team. To prevent creating multiple members with the same name in the same
     * team.
     */
    public static class MemberNameValidator implements InputValidator { // NO_UCD (use private)

        public MemberNameValidator() {}

        @Override
        public String getError(Context context, CharSequence input, Bundle extras) {
            long teamId = extras.getLong(Teams.EXTRA_TEAM_ID);
            String currentMemberName = extras.getString(EXTRA_MEMBER_NAME);
            if (!TextUtils.isEmpty(currentMemberName) && currentMemberName.equals(input)) return null;

            // Query for a member with this name.
            Cursor existingMemberCountCursor = context.getContentResolver().query(MemberColumns.CONTENT_URI, new String[] { "count(*)" },
                    MemberColumns.NAME + "=? AND " + MemberColumns.TEAM_ID + "=? AND " + MemberColumns.DELETED + "=0", new String[] { String.valueOf(input), String.valueOf(teamId) }, null);

            // Now Check if the team member exists.
            if (existingMemberCountCursor != null) {
                if (existingMemberCountCursor.moveToFirst()) {
                    int existingMemberCount = existingMemberCountCursor.getInt(0);
                    existingMemberCountCursor.close();
                    if (existingMemberCount > 0) return context.getString(R.string.error_member_exists, input);
                }
            }
            return null;
        }
    }
}
