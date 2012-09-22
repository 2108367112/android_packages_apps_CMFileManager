/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.explorer.model;

import java.io.Serializable;

/**
 * Permissions of a filesystem object.
 *
 * @see Permission
 * @see FileSystemObject
 */
public class Permissions implements Serializable, Comparable<Permissions> {

    private static final long serialVersionUID = 297676577053698527L;

    private UserPermission mUser;
    private GroupPermission mGroup;
    private OthersPermission mOthers;

    /**
     * Constructor of <code>Permissions</code>.
     *
     * @param user The permissions for the proprietary user of the filesystem object
     * @param group The permissions for the proprietary group of the filesystem object
     * @param others The permissions for the non proprietary users of the filesystem object
     */
    public Permissions(UserPermission user, GroupPermission group, OthersPermission others) {
        super();
        this.mUser = user;
        this.mGroup = group;
        this.mOthers = others;
    }

    /**
     * Method that returns the permissions for the proprietary user of the filesystem object.
     *
     * @return UserPermission The permissions for the proprietary user of the filesystem object
     */
    public UserPermission getUser() {
        return this.mUser;
    }

    /**
     * Method that returns the permissions for the proprietary user of the filesystem object.
     *
     * @param user The permissions for the proprietary user of the filesystem object
     */
    public void setUser(UserPermission user) {
        this.mUser = user;
    }

    /**
     * Method that returns the permissions for the proprietary group of the filesystem object.
     *
     * @return GroupPermission The permissions for the proprietary group of the filesystem object
     */
    public GroupPermission getGroup() {
        return this.mGroup;
    }

    /**
     * Method that returns the permissions for the proprietary group of the filesystem object.
     *
     * @param group The permissions for the proprietary group of the filesystem object
     */
    public void setGroup(GroupPermission group) {
        this.mGroup = group;
    }

    /**
     * Method that returns the permissions for the non proprietary users of the filesystem object.
     *
     * @return Permission The permissions for the non proprietary users of the filesystem object
     */
    public OthersPermission getOthers() {
        return this.mOthers;
    }

    /**
     * Method that returns the permissions for the non proprietary users of the filesystem object.
     *
     * @param others The permissions for the non proprietary users of the filesystem object
     */
    public void setOthers(OthersPermission others) {
        this.mOthers = others;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Permissions another) {
        String o1 = this.toRawString();
        String o2 = another.toRawString();
        return o1.compareTo(o2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.mGroup == null) ? 0 : this.mGroup.hashCode());
        result = prime * result + ((this.mOthers == null) ? 0 : this.mOthers.hashCode());
        result = prime * result + ((this.mUser == null) ? 0 : this.mUser.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Permissions other = (Permissions) obj;
        if (this.mGroup == null) {
            if (other.mGroup != null) {
                return false;
            }
        } else if (!this.mGroup.equals(other.mGroup)) {
            return false;
        }
        if (this.mOthers == null) {
            if (other.mOthers != null) {
                return false;
            }
        } else if (!this.mOthers.equals(other.mOthers)) {
            return false;
        }
        if (this.mUser == null) {
            if (other.mUser != null) {
                return false;
            }
        } else if (!this.mUser.equals(other.mUser)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Permissions [user=" + this.mUser //$NON-NLS-1$
                + ", group=" + this.mGroup +  //$NON-NLS-1$
                ", others=" + this.mOthers + "]"; //$NON-NLS-1$//$NON-NLS-2$
    }

    /**
     * Method that returns a string representation of the permissions,
     * conforming with the unix style (rwx).
     *
     * @return String The string representation of the permissions
     */
    public String toRawString() {
        return String.format("%s%s%s", //$NON-NLS-1$
                    this.mUser.toRawString(),
                    this.mGroup.toRawString(),
                    this.mOthers.toRawString());
    }

    /**
     * Method that converts every permission into octal numbers,
     * conforming with the unix style (xe: 0755).
     *
     * @return String The octal numbers string for the permissions
     */
    @SuppressWarnings("boxing")
    public String toOctalString() {
        //SetUID/SetGID/Sticky Bit
        int b = 0;
        if (this.mUser.isSetUID()) {
            b = b | 0x04;
        }
        if (this.mGroup.isSetGID()) {
            b = b | 0x02;
        }
        if (this.mOthers.isStickybit()) {
            b = b | 0x01;
        }
        //User
        int u = 0;
        if (this.mUser.isRead()) {
            u = u | 0x04;
        }
        if (this.mUser.isWrite()) {
            u = u | 0x02;
        }
        if (this.mUser.isExecute()) {
            u = u | 0x01;
        }
        //Group
        int g = 0;
        if (this.mGroup.isRead()) {
            g = g | 0x04;
        }
        if (this.mGroup.isWrite()) {
            g = g | 0x02;
        }
        if (this.mGroup.isExecute()) {
            g = g | 0x01;
        }
        //Others
        int o = 0;
        if (this.mOthers.isRead()) {
            o = o | 0x04;
        }
        if (this.mOthers.isWrite()) {
            o = o | 0x02;
        }
        if (this.mOthers.isExecute()) {
            o = o | 0x01;
        }

        //Return octal string
        return String.format("%d%d%d%d", b, u, g, o); //$NON-NLS-1$
    }

}
