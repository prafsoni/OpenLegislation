package gov.nysenate.openleg.controller.api.admin;

import com.google.common.collect.Range;
import gov.nysenate.openleg.client.response.base.BaseResponse;
import gov.nysenate.openleg.client.response.base.ListViewResponse;
import gov.nysenate.openleg.client.response.base.ViewObjectResponse;
import gov.nysenate.openleg.client.view.notification.NotificationSummaryView;
import gov.nysenate.openleg.client.view.notification.NotificationView;
import gov.nysenate.openleg.controller.api.base.BaseCtrl;
import gov.nysenate.openleg.controller.api.base.InvalidRequestParamEx;
import gov.nysenate.openleg.dao.base.LimitOffset;
import gov.nysenate.openleg.dao.base.PaginatedList;
import gov.nysenate.openleg.dao.base.SortOrder;
import gov.nysenate.openleg.dao.notification.NotificationDao;
import gov.nysenate.openleg.model.notification.NotificationType;
import gov.nysenate.openleg.model.notification.RegisteredNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.nysenate.openleg.controller.api.base.BaseCtrl.BASE_ADMIN_API_PATH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = BASE_ADMIN_API_PATH + "/notifications", method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
public class NotificationCtrl extends BaseCtrl
{
    @Autowired
    private NotificationDao notificationDao;

    /**
     * Single Notification Retrieval API
     * ---------------------------------
     *
     * Retrieve a single notification by id (GET) /api/3/admin/notifications/{id}
     *
     * <p>Request Parameters: None</p>
     *
     * Expected Output: NotificationView
     */
    @RequestMapping(value = "/{id:\\d+}")
    public BaseResponse getNotification(@PathVariable int id) {
        return new ViewObjectResponse<>(new NotificationView(notificationDao.getNotification(id)));
    }

    /**
     * Notification Listing API
     * ------------------------
     *
     * Return notifications from the past week (GET) /api/3/admin/notifications
     * Request Params: type (string) - NotificationType. Default: ALL
     *                 full (boolean) - If true, NotificationView is returned, otherwise NotificationSummaryView.
     *                 limit, offset (int) - Paginate.
     *                 order (string) - Order by update date.
     *
     * Expected Output: List of NotificationSummaryView or NotificationView
     */
    @RequestMapping(value = "")
    public BaseResponse getNotifications(WebRequest request) {
        LocalDateTime fromDate = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime toDate = LocalDateTime.now();
        return getNotificationsDuring(fromDate, toDate, request);
    }

    /**
     * Notification Listing API
     * ------------------------
     *
     * Return notifications from a given date to now (GET) /api/3/admin/notifications/{from}
     * @see #getNotifications(WebRequest)
     */
    @RequestMapping(value = "/{from:\\d{4}-.*}")
    public BaseResponse getNotifications(@PathVariable String from, WebRequest request) {
        LocalDateTime fromDate = parseISODateTime(from, "from");
        LocalDateTime toDate = LocalDateTime.now();
        return getNotificationsDuring(fromDate, toDate, request);
    }

    /**
     * Notification Listing API
     * ------------------------
     *
     * Return notifications for a given date/time range (GET) /api/3/admin/notifications/{from}/{to}
     * @see #getNotifications(WebRequest)
     */
    @RequestMapping(value = "/{from}/{to}")
    public BaseResponse getNotifications(@PathVariable String from,
                                         @PathVariable String to,
                                         WebRequest request) {
        LocalDateTime fromDate = parseISODateTime(from, "from");
        LocalDateTime toDate = parseISODateTime(to, "to");
        return getNotificationsDuring(fromDate, toDate, request);
    }


    /** --- Internal --- */

    private BaseResponse getNotificationsDuring(LocalDateTime from, LocalDateTime to, WebRequest request) {
        Range<LocalDateTime> dateRange = Range.openClosed(from, to);
        LimitOffset limOff = getLimitOffset(request, 25);
        SortOrder order = getSortOrder(request, SortOrder.DESC);
        boolean full = getBooleanParam(request, "full", false);
        PaginatedList<RegisteredNotification> results =
                notificationDao.getNotifications(getNotificationTypes(request), dateRange, order, limOff);
        return ListViewResponse.of(results.getResults().stream().map(
                (full) ? NotificationView::new : NotificationSummaryView::new)
                .collect(Collectors.toList()), results.getTotal(), limOff);
    }

    private Set<NotificationType> getNotificationTypes(WebRequest request) {
        List<String> types = Arrays.asList(request.getParameterValues("type"));
        Set<NotificationType> typeSet = new HashSet<>();
        for (String type : types) {
            try {
                typeSet.addAll(NotificationType.getCoverage(NotificationType.getValue(type)));
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestParamEx(type, "type", "String",
                        NotificationType.getAllNotificationTypes().stream()
                                .map(NotificationType::toString)
                                .reduce("", (a, b) -> a + "|" + b));
            }
        }
        if (typeSet.size() == 0) {
            typeSet = NotificationType.getAllNotificationTypes();
        }
        return typeSet;
    }
}
