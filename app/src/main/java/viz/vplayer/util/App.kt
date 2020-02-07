package viz.vplayer.util

import android.app.Application
import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.Configuration
import androidx.work.WorkManager
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.AriaConfig
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.QbSdk.PreInitCallback
import com.tencent.stat.StatConfig
import com.tencent.stat.StatCrashCallback
import com.tencent.stat.StatCrashReporter
import com.tencent.stat.StatService
import com.viz.tools.Toast
import com.viz.tools.l
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import viz.vplayer.room.AppDatabase

class App : Application(), Configuration.Provider {
    lateinit var db: AppDatabase

    override fun onCreate() {
        super.onCreate()
        l.init(this)
        l.SUFFIX = ".kt"
        Toast.init(applicationContext)
        IjkPlayerManager.setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT)
        // [可选]设置是否打开debug输出，上线时请关闭，Logcat标签为"MtaSDK"
//        StatConfig.setDebugEnable(true)
        // 基础统计API
        StatService.registerActivityLifecycleCallbacks(this)
        StatCrashReporter.getStatCrashReporter(applicationContext).javaCrashHandlerStatus = true
        StatCrashReporter.getStatCrashReporter(applicationContext).jniNativeCrashStatus = true
        StatCrashReporter.getStatCrashReporter(applicationContext).addCrashCallback(
            object : StatCrashCallback {
                override fun onJniNativeCrash(nativeCrashStacks: String) { // native crash happened
                    Log.e("nativeCrash", nativeCrashStacks)
                }

                override fun onJavaCrash(
                    thread: Thread,
                    ex: Throwable
                ) { // java crash happened
                    ex.printStackTrace()
                }
            })
        StatCrashReporter.getStatCrashReporter(applicationContext).isEnableInstantReporting = true
        instance = this
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE videoinfo ADD COLUMN video_title TEXT NOT NULL DEFAULT ''")
                database.execSQL("create table `episode`  (`id` INTEGER NOT NULL, `video_id` INTEGER NOT NULL, `url` TEXT NOT NULL,`url_index` INTEGER NOT NULL, PRIMARY KEY(`id`),FOREIGN KEY (`video_id`) REFERENCES VideoInfo(`id`))")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("create table `rule` (`rule_enable` INTEGER NOT NULL DEFAULT 1,`id` INTEGER NOT NULL,`rule_url` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE videoinfo ADD COLUMN duration INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE videoinfo ADD COLUMN video_img_url TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE rule ADD COLUMN rule_status INTEGER NOT NULL DEFAULT 1")
            }
        }
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-vplayer"
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                RoomUtil.migration(
                    5,
                    6,
                    "create table download (`notification_id` INTEGER NOT NULL DEFAULT 1,`id` INTEGER NOT NULL,`video_url` TEXT NOT NULL, PRIMARY KEY(`id`))"
                ),
                RoomUtil.migration(
                    6,
                    7,
                    "ALTER TABLE download ADD COLUMN download_status INTEGER NOT NULL DEFAULT 0"
                ),
                RoomUtil.migration(
                    7,
                    8,
                    "ALTER TABLE download ADD COLUMN video_img_url TEXT NOT NULL DEFAULT ''",
                    "ALTER TABLE download ADD COLUMN video_title TEXT NOT NULL DEFAULT ''"
                ),
                RoomUtil.migration(
                    8,
                    9,
                    "ALTER TABLE download ADD COLUMN download_progress INTEGER NOT NULL DEFAULT 0"
                ),
                RoomUtil.migration(
                    9,
                    10,
                    "ALTER TABLE videoinfo ADD COLUMN search_url TEXT NOT NULL DEFAULT ''"
                ),
                RoomUtil.migration(
                    10,
                    11,
                    "ALTER TABLE download ADD COLUMN duration INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE download ADD COLUMN search_url TEXT NOT NULL DEFAULT ''"
                )
            )
            .build()
        //搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核。

        //搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核。
        val cb: PreInitCallback = object : PreInitCallback {
            override fun onViewInitFinished(arg0: Boolean) { // TODO Auto-generated method stub
//x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                l.d("app", " onViewInitFinished is $arg0")
            }

            override fun onCoreInitFinished() { // TODO Auto-generated method stub
            }
        }
        //x5内核初始化接口
        //x5内核初始化接口
        QbSdk.initX5Environment(applicationContext, cb)
    }

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    override fun onTerminate() {
        super.onTerminate()
        WorkManager.getInstance(this).pruneWork()
    }

    companion object {
        lateinit var instance: App
            private set
    }
}