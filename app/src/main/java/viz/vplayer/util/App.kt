package viz.vplayer.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.Configuration
import androidx.work.WorkManager
import bolts.Task
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig
import com.github.moduth.blockcanary.BlockCanary
import com.huawei.agconnect.AGConnectInstance
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.agconnect.config.LazyInputStream
import com.huawei.hms.push.HmsMessaging
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager
import com.shuyu.gsyvideoplayer.utils.CommonUtil
import com.silencedut.fpsviewer.FpsViewer
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.QbSdk.PreInitCallback
import com.viz.tools.Toast
import com.viz.tools.l
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import org.greenrobot.eventbus.EventBus
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import viz.vplayer.dagger2.AppComponent
import viz.vplayer.dagger2.DaggerAppComponent
import viz.vplayer.eventbus.InitEvent
import viz.vplayer.eventbus.TBSEvent
import viz.vplayer.eventbus.enum.INIT_TYPE
import viz.vplayer.room.AppDatabase
import viz.vplayer.ui.activity.MainActivity
import java.io.IOException
import java.io.InputStream

/**
 * google id:ca-app-pub-2409784170808286~8299617415
 */
class App : DaggerApplication(), Configuration.Provider {
    lateinit var db: AppDatabase
    var appComponent: AppComponent? = null
    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        appComponent = DaggerAppComponent.builder().create(this) as AppComponent?
        return appComponent!!
    }

    override fun onCreate() {
        super.onCreate()
        l.init(this)
        l.SUFFIX = ".kt"
        Toast.init(applicationContext)
        IjkPlayerManager.setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT)
        instance = this
        l.start("room")
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
                ),
                RoomUtil.migration(
                    11,
                    12,
                    "create table m3u8 (`status` INTEGER NOT NULL DEFAULT 1,`progress` INTEGER NOT NULL DEFAULT 1,`id` INTEGER NOT NULL,`url` TEXT NOT NULL, `path` TEXT NOT NULL, PRIMARY KEY(`id`))",
                    "create table ts (`status` INTEGER NOT NULL DEFAULT 1,`m3u8_id` INTEGER NOT NULL DEFAULT 1,`index` INTEGER NOT NULL DEFAULT 1,`progress` INTEGER NOT NULL DEFAULT 1,`id` INTEGER NOT NULL,`url` TEXT NOT NULL, `path` TEXT NOT NULL, PRIMARY KEY(`id`))"
                ),
                RoomUtil.migrationOneVersion(
                    12,
                    "ALTER TABLE download ADD COLUMN work_id TEXT NOT NULL DEFAULT ''"
                ),
                RoomUtil.migrationOneVersion(
                    13,
                    "ALTER TABLE ts ADD COLUMN duration FLOAT NOT NULL DEFAULT 0.0"
                )
            )
            .build()
        l.end("room")

        Task.callInBackground {
            l.start("tbs")
            //搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核。
            val cb: PreInitCallback = object : PreInitCallback {
                override fun onViewInitFinished(arg0: Boolean) { // TODO Auto-generated method stub
//x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                    l.d("app", " onViewInitFinished is $arg0")
                    if(!arg0) {
                        EventBus.getDefault().postSticky(TBSEvent(0))
                    }
                }

                override fun onCoreInitFinished() { // TODO Auto-generated method stub
                }
            }
            //x5内核初始化接口
            QbSdk.initX5Environment(applicationContext, cb)
            l.end("tbs")
            EventBus.getDefault().postSticky(InitEvent(INIT_TYPE.TBS))
        }.continueWithEnd("初始化tbs")

        l.start("baidu mtj")
        com.baidu.mobstat.StatService.autoTrace(this)
        l.end("baidu mtj")
//        Task.callInBackground {
//            l.start("copyAssetX5")
//            copyBigDataToSD(this, FileUtil.getPath(this)+"/backup/x5.tbs.org", "x5/x5.tbs.org", "", "")
//            l.end("copyAssetX5")
//        }.continueWithEnd("复制内核")

        Task.callInBackground {
            l.start("fresco")
            val progressiveJpegConfig = SimpleProgressiveJpegConfig()
            val config = ImagePipelineConfig.newBuilder(this)
//            .setBitmapMemoryCacheParamsSupplier(bitmapCacheParamsSupplier)
//            .setCacheKeyFactory(cacheKeyFactory)
                .setDownsampleEnabled(true)
//            .setWebpSupportEnabled(true)
//            .setEncodedMemoryCacheParamsSupplier(encodedCacheParamsSupplier)
//            .setExecutorSupplier(executorSupplier)
//            .setImageCacheStatsTracker(imageCacheStatsTracker)
//            .setMainDiskCacheConfig(mainDiskCacheConfig)
//            .setMemoryTrimmableRegistry(memoryTrimmableRegistry)
//            .setNetworkFetchProducer(networkFetchProducer)
//            .setPoolFactory(poolFactory)
                .setProgressiveJpegConfig(progressiveJpegConfig)
//            .setRequestListeners(requestListeners)
//            .setSmallImageDiskCacheConfig(smallImageDiskCacheConfig)
                .build()
            Fresco.initialize(this, config)
            l.end("fresco")
        }.continueWithEnd("初始化Fresco")

        l.start("HmsMessaging")
        HmsMessaging.getInstance(this).isAutoInitEnabled = true
        l.end("HmsMessaging")

        CrashReport.initCrashReport(applicationContext, "27b662f818", false)
        Thread.setDefaultUncaughtExceptionHandler(handler)
    }

    private val handler = Thread.UncaughtExceptionHandler { t, e ->
        e.printStackTrace()
        l.e("发生异常,重启应用")
        restartApp() //发生崩溃异常时,重启应用
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        val restartIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT
        )
        //退出程序
        val mgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mgr.set(
            AlarmManager.RTC, System.currentTimeMillis() + 1000,
            restartIntent
        ) // 1秒钟后重启应用

        //结束进程之前可以把你程序的注销或者退出代码放在这段代码之前
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        l.start("agconnect")
        val agConnectServicesConfig: AGConnectServicesConfig =
            AGConnectServicesConfig.fromContext(base)
        agConnectServicesConfig.overlayWith(object : LazyInputStream(base) {
            override operator fun get(context: Context): InputStream? {
                return try {
                    context.assets.open("agconnect-services.json")
                } catch (e: IOException) {
                    null
                }
            }
        })
        l.end("agconnect")
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