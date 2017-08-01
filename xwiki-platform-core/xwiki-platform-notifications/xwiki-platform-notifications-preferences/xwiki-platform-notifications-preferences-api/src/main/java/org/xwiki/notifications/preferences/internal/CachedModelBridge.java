/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.notifications.preferences.internal;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.preferences.NotificationPreference;

/**
 * Wrap the default {@link ModelBridge} to store in the execution context the notification preferences to avoid
 * fetching them several time during the same HTTP request.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Named("cached")
@Singleton
public class CachedModelBridge implements ModelBridge
{
    private static final String USER_NOTIFICATIONS_PREFERENCES = "userNotificationsPreferences";

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private Execution execution;

    @Override
    public List<NotificationPreference> getNotificationsPreferences(DocumentReference userReference)
            throws NotificationException
    {
        ExecutionContext context = execution.getContext();
        if (context.hasProperty(USER_NOTIFICATIONS_PREFERENCES)) {
            return (List<NotificationPreference>) context.getProperty(USER_NOTIFICATIONS_PREFERENCES);
        }

        List<NotificationPreference> preferences = modelBridge.getNotificationsPreferences(userReference);
        context.setProperty(USER_NOTIFICATIONS_PREFERENCES, preferences);

        return preferences;
    }

    @Override
    public void setStartDateForUser(DocumentReference userReference, Date startDate)
            throws NotificationException
    {
        // Obviously, there is no possible cache here
        modelBridge.setStartDateForUser(userReference, startDate);
    }

    @Override
    public void saveNotificationsPreferences(DocumentReference userReference,
            List<NotificationPreference> notificationPreferences) throws NotificationException
    {
        // Obviously there is nothing to cache
        modelBridge.saveNotificationsPreferences(userReference, notificationPreferences);
    }
}
