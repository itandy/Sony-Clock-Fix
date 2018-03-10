package com.gzplanet.xposed.semcclockfix;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.getLongField;

public class SemcClockFix implements IXposedHookLoadPackage {
    final static String TAG = "SemcClockFix";
    final static String PKGNAME = "com.sonyericsson.organizer";
    final static String ALARM_CLASSNAME = "com.sonyericsson.alarm";
    final static String NOTIFICATIONHELPER_CLASSNAME = "com.sonyericsson.organizer.utils";

    static Class<?> alarmClass = null;
    static Class<?> alarmsClass = null;
    static Class<?> alarmReceiverClass = null;
    static Class<?> alarmInitReceiverClass = null;
    static Class<?> notificationHelperClass = null;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(PKGNAME))
            return;

        XposedBridge.log(TAG + ": Loaded package " + PKGNAME);

        try {
            alarmClass = XposedHelpers.findClass(ALARM_CLASSNAME + ".Alarm", lpparam.classLoader);
            alarmsClass = XposedHelpers.findClass(ALARM_CLASSNAME + ".Alarms", lpparam.classLoader);
            alarmReceiverClass = XposedHelpers.findClass(ALARM_CLASSNAME + ".AlarmReceiver", lpparam.classLoader);
            alarmInitReceiverClass = XposedHelpers.findClass(ALARM_CLASSNAME + ".AlarmInitReceiver", lpparam.classLoader);
            notificationHelperClass = XposedHelpers.findClass(NOTIFICATIONHELPER_CLASSNAME + ".NotificationHelper", lpparam.classLoader);


            XposedHelpers.findAndHookMethod(alarmsClass, "addAlarm", Context.class, alarmClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + ": Alarms:addAlarm");
                }
            });

            XposedHelpers.findAndHookMethod(alarmsClass, "enableAlarm", Context.class, int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + ": Alarms:enableAlarm");
                }
            });

            XposedHelpers.findAndHookMethod(alarmsClass, "setAlarm", Context.class, alarmClass, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + ": Alarms:setAlarm");
                }
            });

            XposedHelpers.findAndHookMethod(alarmsClass, "dismissAlarm", Context.class, alarmClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + ": Alarms:dismissAlarm");
                    setUpcomingNotification((Context) param.args[0]);
                }
            });

            XposedHelpers.findAndHookMethod(alarmsClass, "setUpcomingNotificationIntents", Context.class, alarmClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + ": Alarms:setUpcomingNotificationIntents");
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    long calculatedTime = XposedHelpers.getLongField(param.args[1], "calculatedTime");
                    XposedBridge.log(TAG + ":    alarm.calculatedTime:" + formatLongTime(calculatedTime));
                }
            });

            XposedHelpers.findAndHookMethod(alarmsClass, "cancelUpcomingNotificationIntents", Context.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + ": Alarms:cancelUpcomingNotificationIntents");
                }
            });

            XposedHelpers.findAndHookMethod(alarmsClass, "setUpcomingIntent", AlarmManager.class, alarmClass,
                    PendingIntent.class, long.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": Alarms:setUpcomingIntent");
                            XposedBridge.log(TAG + ":    n:" + Long.valueOf(param.args[3].toString()));
                        }
                    });

            XposedHelpers.findAndHookMethod(alarmsClass, "shouldNotifyUpcomingAlarm", alarmClass, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log(TAG + ": Alarms:shouldNotifyUpcomingAlarm");
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    long calculatedTime = XposedHelpers.getLongField(param.args[0], "calculatedTime");
                    XposedBridge.log(TAG + ":    alarm.calculatedTime:" + formatLongTime(calculatedTime));
                }
            });

            XposedHelpers.findAndHookMethod(alarmReceiverClass, "onReceive", Context.class, Intent.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": AlarmReceiver:onReceive");
                            Intent intent = (Intent)param.args[1];
                            XposedBridge.log(TAG + ":    action:" + intent.getAction());
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Intent intent = (Intent)param.args[1];
                            if (intent.getAction().equals("com.sonyericsson.alarm.ALARM_RESET"))
                                setUpcomingNotification((Context) param.args[0]);
                        }
                    });

            XposedHelpers.findAndHookMethod(alarmReceiverClass, "notifyUpcomingAlarm", Context.class, alarmClass,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": AlarmReceiver:notifyUpcomingAlarm(Context,Alarm)");
                        }
                    });

            XposedHelpers.findAndHookMethod(alarmReceiverClass, "notifyUpcomingAlarm", Context.class, NotificationManager.class, alarmClass,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": AlarmReceiver:notifyUpcomingAlarm(Context,NotificationManager,Alarm)");
                        }
                    });

            XposedHelpers.findAndHookMethod(alarmInitReceiverClass, "onReceive", Context.class, Intent.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": AlarmInitReceiver:onReceive");
                            Intent intent = (Intent) param.args[1];
                            XposedBridge.log(TAG + ":    " + intent.getAction());
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Intent intent = (Intent) param.args[1];
                            if (intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
                                setUpcomingNotification((Context) param.args[0]);
                            }
                        }
                    });
        } catch (XposedHelpers.ClassNotFoundError e) {
            XposedBridge.log(TAG + ": ClassNotFoundError " + e.getMessage());
        } catch (NoSuchMethodError e) {
            XposedBridge.log(TAG + ": NoSuchMethodError " + e.getMessage());
        }

    }

    private static String formatLongTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return formatDate(calendar.getTime(), "yyyy-MM-dd HH:mm:ss");
    }

    private static String formatDate(Date date, String mask) {
        if (date == null)
            return "";
        SimpleDateFormat sdt = new SimpleDateFormat(mask);
        return sdt.format(date);
    }

    private static void setUpcomingNotification(Context context) {
        try {
            List<Object> alarms = (List<Object>) XposedHelpers.callStaticMethod(alarmsClass, "getEnabledAlarms", context);
            if (alarms != null) {
                XposedBridge.log(TAG + ":    size:" + alarms.size());
                if (alarms.size() > 0) {
                    Object notificationHelper = XposedHelpers.callStaticMethod(notificationHelperClass, "getInstance");
                    for (int i = 0; i < alarms.size(); i++) {
                        long calculatedTime = XposedHelpers.getLongField(alarms.get(i), "calculatedTime");
                        int id = XposedHelpers.getIntField(alarms.get(i), "id");
                        XposedBridge.log(TAG + String.format(":    id:%d time:%s", id, formatLongTime(calculatedTime)));

                        // cancel previous notification intent, if any
                        XposedHelpers.callStaticMethod(alarmsClass, "cancelUpcomingNotificationIntents", context, id);

                        // cancel previous notification intent, if any
                        if (notificationHelper != null)
                            XposedHelpers.callMethod(notificationHelper, "cancel", "com.sonyericsson.organizer.upcoming_alarm_group_key", id);

                        // create new notification
                        XposedHelpers.callStaticMethod(alarmsClass, "setUpcomingNotificationIntents", context, alarms.get(i));
                    }
                }
            }
        } catch (Exception e) {
            XposedBridge.log(e.getMessage());
        }
    }
}
