package com.example.domain.model

data class SmartAppTemplate(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val badgeAr: String,
    val accentHex: String,
    val secondaryHex: String,
    val iconKey: String,
    val featureCountLabel: String,
    val buildProject: () -> AppProject
)

object SmartTemplatesCatalog {

    fun getAll(): List<SmartAppTemplate> {
        val now = System.currentTimeMillis()
        return listOf(
            SmartAppTemplate(
                id = "tpl_store",
                titleAr = "متجر إلكتروني 3D",
                subtitleAr = "سلة تسوق، عروض تفاعلية، تنبيهات طلبات، وواتساب",
                badgeAr = "تجارة",
                accentHex = "#FF7A2F",
                secondaryHex = "#7C3AED",
                iconKey = "store",
                featureCountLabel = "24 ميزة",
                buildProject = {
                    AppProject(
                        id = "proj_store_$now",
                        name = "متجري برو",
                        description = "تطبيق متجر إلكتروني متكامل مع تنبيهات الطلبات ودعم الدفع والواتساب",
                        primaryColorHex = "#FF7A2F",
                        accentColorHex = "#FF7A2F",
                        sourceType = SourceType.HTML_FOLDER,
                        sourceValue = "store_template.html",
                        splashConfig = SplashConfig(
                            enabled = true,
                            backgroundColorHex = "#12100E",
                            indicatorStyle = "حلقة"
                        ),
                        onboardingConfig = OnboardingConfig(
                            enabled = true,
                            slides = listOf(
                                OnboardingSlide("تسوق بسهولة", "تصفح أحدث المنتجات والعروض الحصرية", "shopping"),
                                OnboardingSlide("تتبع لحظي", "إشعارات فورية بحالة طلبك حتى باب المنزل", "notifications")
                            ),
                            accentColorHex = "#FF7A2F"
                        ),
                        webViewConfig = WebViewConfig(
                            fullScreen = false,
                            pullToRefresh = true,
                            cacheEnabled = true,
                            offlineFallback = true
                        ),
                        bottomNavConfig = BottomNavConfig(
                            enabled = true,
                            activeColorHex = "#FF7A2F",
                            destinations = listOf(
                                BottomNavDestination("home", "الرئيسية", "/", "home"),
                                BottomNavDestination("cats", "الأقسام", "/categories", "apps"),
                                BottomNavDestination("cart", "السلة", "/cart", "shopping_cart"),
                                BottomNavDestination("acc", "حسابي", "/profile", "person")
                            )
                        ),
                        packageConfig = PackageConfig(
                            packageName = "com.apkforge.mystore",
                            versionName = "1.0.0",
                            versionCode = 1,
                            minSdk = 24,
                            targetSdk = 34
                        ),
                        permissionsConfig = PermissionsConfig(
                            camera = true,
                            notifications = true,
                            vibration = true,
                            networkState = true
                        ),
                        pushConfig = PushNotificationConfig(
                            enabled = true,
                            provider = "OneSignal",
                            channelName = "عروض وطلبات المتجر"
                        ),
                        bridgeConfig = BridgeAndCloudConfig(
                            jsBridgeEnabled = true,
                            handleExternalIntents = true,
                            deepLinkDomain = "shop.apkforge.app",
                            gitHubCiEnabled = true
                        ),
                        omniConfig = OmniIdeConfig(
                            gradientEnabled = true,
                            gradientStartHex = "#FF7A2F",
                            gradientEndHex = "#7C3AED",
                            localDbEnabled = true,
                            localDbTableName = "store_cart"
                        )
                    )
                }
            ),
            SmartAppTemplate(
                id = "tpl_restaurant",
                titleAr = "مطعم ومنيو ذكي",
                subtitleAr = "قائمة وجبات، طلب سريع، خريطة GPS، وحجز طاولات",
                badgeAr = "مطاعم",
                accentHex = "#F59E0B",
                secondaryHex = "#DC2626",
                iconKey = "restaurant",
                featureCountLabel = "20 ميزة",
                buildProject = {
                    AppProject(
                        id = "proj_rest_$now",
                        name = "مذاق بلس",
                        description = "تطبيق مطعم متكامل مع منيو رقمي وتوصيل فوري",
                        primaryColorHex = "#F59E0B",
                        accentColorHex = "#F59E0B",
                        sourceType = SourceType.HTML_FOLDER,
                        sourceValue = "restaurant_menu.html",
                        splashConfig = SplashConfig(
                            enabled = true,
                            backgroundColorHex = "#17110B",
                            indicatorStyle = "نبض"
                        ),
                        onboardingConfig = OnboardingConfig(
                            enabled = true,
                            slides = listOf(
                                OnboardingSlide("أشهى الأطباق", "تصفح المنيو المصور والوجبات اليومية الطازجة", "restaurant"),
                                OnboardingSlide("توصيل سريع", "حدد موقعك واطلب وجبتك بضغطة زر", "bolt")
                            ),
                            accentColorHex = "#F59E0B"
                        ),
                        webViewConfig = WebViewConfig(
                            fullScreen = false,
                            pullToRefresh = true,
                            cacheEnabled = true,
                            offlineFallback = true
                        ),
                        bottomNavConfig = BottomNavConfig(
                            enabled = true,
                            activeColorHex = "#F59E0B",
                            destinations = listOf(
                                BottomNavDestination("menu", "المنيو", "/", "home"),
                                BottomNavDestination("offers", "العروض", "/offers", "explore"),
                                BottomNavDestination("orders", "طلباتي", "/orders", "shopping_cart"),
                                BottomNavDestination("branch", "الفروع", "/branches", "person")
                            )
                        ),
                        packageConfig = PackageConfig(
                            packageName = "com.apkforge.restaurant",
                            versionName = "1.0.0",
                            versionCode = 1,
                            minSdk = 24,
                            targetSdk = 34
                        ),
                        permissionsConfig = PermissionsConfig(
                            location = true,
                            notifications = true,
                            vibration = true,
                            networkState = true
                        ),
                        proConfig = ProStudioConfig(
                            fabMenuEnabled = true,
                            fabWhatsappNumber = "966500000000"
                        )
                    )
                }
            ),
            SmartAppTemplate(
                id = "tpl_portfolio",
                titleAr = "محفظة أعمال (Portfolio)",
                subtitleAr = "معرض مشاريع 3D، سيرة ذاتية، وتواصل مباشر",
                badgeAr = "أعمال",
                accentHex = "#EC4899",
                secondaryHex = "#6366F1",
                iconKey = "portfolio",
                featureCountLabel = "16 ميزة",
                buildProject = {
                    AppProject(
                        id = "proj_port_$now",
                        name = "إبداعي 3D",
                        description = "محفظة أعمال شخصية ومشاريع إبداعية بتصميم ثلاثي الأبعاد",
                        primaryColorHex = "#EC4899",
                        accentColorHex = "#EC4899",
                        sourceType = SourceType.HTML_FOLDER,
                        sourceValue = "portfolio_3d.html",
                        splashConfig = SplashConfig(
                            enabled = true,
                            backgroundColorHex = "#140D1B",
                            indicatorStyle = "حلقة"
                        ),
                        webViewConfig = WebViewConfig(
                            fullScreen = true,
                            pullToRefresh = true,
                            cacheEnabled = true,
                            offlineFallback = true
                        ),
                        bottomNavConfig = BottomNavConfig(
                            enabled = true,
                            activeColorHex = "#EC4899",
                            destinations = listOf(
                                BottomNavDestination("work", "أعمالي", "/", "home"),
                                BottomNavDestination("services", "الخدمات", "/services", "apps"),
                                BottomNavDestination("contact", "تواصل", "/contact", "person")
                            )
                        ),
                        packageConfig = PackageConfig(
                            packageName = "com.apkforge.portfolio",
                            versionName = "1.0.0",
                            versionCode = 1,
                            minSdk = 24,
                            targetSdk = 34
                        ),
                        omniConfig = OmniIdeConfig(
                            gradientEnabled = true,
                            gradientStartHex = "#EC4899",
                            gradientEndHex = "#6366F1"
                        ),
                        proConfig = ProStudioConfig(
                            drawerEnabled = true,
                            drawerHeaderSubtitle = "معرض الأعمال والمهارات",
                            fabMenuEnabled = true
                        )
                    )
                }
            ),
            SmartAppTemplate(
                id = "tpl_news",
                titleAr = "مدونة وأخبار",
                subtitleAr = "قراءة سريعة، حفظ أوفلاين، وإشعارات عاجلة",
                badgeAr = "إعلام",
                accentHex = "#38BDF8",
                secondaryHex = "#0EA5E9",
                iconKey = "blog",
                featureCountLabel = "18 ميزة",
                buildProject = {
                    AppProject(
                        id = "proj_news_$now",
                        name = "نبض الأخبار",
                        description = "تطبيق أخبار ومقالات سريع مع تخزين مؤقت للقراءة دون اتصال",
                        primaryColorHex = "#38BDF8",
                        accentColorHex = "#38BDF8",
                        sourceType = SourceType.HTML_FOLDER,
                        sourceValue = "news_portal.html",
                        splashConfig = SplashConfig(
                            enabled = true,
                            backgroundColorHex = "#0B1220",
                            indicatorStyle = "شريط"
                        ),
                        webViewConfig = WebViewConfig(
                            fullScreen = false,
                            pullToRefresh = true,
                            cacheEnabled = true,
                            offlineFallback = true
                        ),
                        bottomNavConfig = BottomNavConfig(
                            enabled = true,
                            activeColorHex = "#38BDF8",
                            destinations = listOf(
                                BottomNavDestination("latest", "العاجل", "/", "home"),
                                BottomNavDestination("sections", "الأقسام", "/topics", "explore"),
                                BottomNavDestination("saved", "المحفوظة", "/bookmarks", "bookmark")
                            )
                        ),
                        packageConfig = PackageConfig(
                            packageName = "com.apkforge.newsportal",
                            versionName = "1.0.0",
                            versionCode = 1,
                            minSdk = 24,
                            targetSdk = 34
                        ),
                        permissionsConfig = PermissionsConfig(
                            notifications = true,
                            networkState = true
                        ),
                        pushConfig = PushNotificationConfig(
                            enabled = true,
                            provider = "OneSignal",
                            channelName = "الأخبار العاجلة"
                        ),
                        bridgeConfig = BridgeAndCloudConfig(
                            jsBridgeEnabled = true,
                            handleExternalIntents = true,
                            deepLinkDomain = "news.apkforge.app",
                            gitHubCiEnabled = true
                        )
                    )
                }
            ),
            SmartAppTemplate(
                id = "tpl_academy",
                titleAr = "منصة تعليمية",
                subtitleAr = "دورات، شاشات ترحيب، ورفع واجبات بالكاميرا",
                badgeAr = "تعليم",
                accentHex = "#A855F7",
                secondaryHex = "#3B82F6",
                iconKey = "academy",
                featureCountLabel = "22 ميزة",
                buildProject = {
                    AppProject(
                        id = "proj_edu_$now",
                        name = "أكاديميتي",
                        description = "تطبيق منصة دورات وتدريب تفاعلية للطلاب والمدربين",
                        primaryColorHex = "#A855F7",
                        accentColorHex = "#A855F7",
                        sourceType = SourceType.HTML_FOLDER,
                        sourceValue = "academy_app.html",
                        splashConfig = SplashConfig(
                            enabled = true,
                            backgroundColorHex = "#140F24",
                            indicatorStyle = "حلقة"
                        ),
                        onboardingConfig = OnboardingConfig(
                            enabled = true,
                            slides = listOf(
                                OnboardingSlide("تعلم في أي وقت", "دورات مسجلة ومحاضرات تفاعلية عالية الدقة", "school"),
                                OnboardingSlide("اختبارات وشهادات", "تابع تقدمك واحصل على شهادات إتمام فورية", "verified")
                            ),
                            accentColorHex = "#A855F7"
                        ),
                        webViewConfig = WebViewConfig(
                            fullScreen = false,
                            pullToRefresh = true,
                            supportZoom = true,
                            cacheEnabled = true,
                            fileDownload = true,
                            offlineFallback = true
                        ),
                        bottomNavConfig = BottomNavConfig(
                            enabled = true,
                            activeColorHex = "#A855F7",
                            destinations = listOf(
                                BottomNavDestination("courses", "دوراتي", "/", "home"),
                                BottomNavDestination("live", "البث المباشر", "/live", "explore"),
                                BottomNavDestination("profile", "شهاداتي", "/certs", "person")
                            )
                        ),
                        packageConfig = PackageConfig(
                            packageName = "com.apkforge.academy",
                            versionName = "1.0.0",
                            versionCode = 1,
                            minSdk = 24,
                            targetSdk = 34
                        ),
                        permissionsConfig = PermissionsConfig(
                            camera = true,
                            microphone = true,
                            notifications = true,
                            storage = true,
                            networkState = true
                        ),
                        bridgeConfig = BridgeAndCloudConfig(
                            jsBridgeEnabled = true,
                            handleExternalIntents = true,
                            gitHubCiEnabled = true
                        )
                    )
                }
            ),
            SmartAppTemplate(
                id = "tpl_booking",
                titleAr = "حجوزات وخدمات",
                subtitleAr = "GPS، اتصال مباشر، وربط واتساب للتأكيد",
                badgeAr = "خدمات",
                accentHex = "#10B981",
                secondaryHex = "#06B6D4",
                iconKey = "booking",
                featureCountLabel = "19 ميزة",
                buildProject = {
                    AppProject(
                        id = "proj_book_$now",
                        name = "موعدي بلس",
                        description = "تطبيق حجز مواعيد وخدمات محلية مع تحديد الموقع الجغرافي",
                        primaryColorHex = "#10B981",
                        accentColorHex = "#10B981",
                        sourceType = SourceType.HTML_FOLDER,
                        sourceValue = "booking_services.html",
                        splashConfig = SplashConfig(
                            enabled = true,
                            backgroundColorHex = "#0B1915",
                            indicatorStyle = "نبض"
                        ),
                        webViewConfig = WebViewConfig(
                            fullScreen = false,
                            pullToRefresh = true,
                            cacheEnabled = true,
                            offlineFallback = true
                        ),
                        bottomNavConfig = BottomNavConfig(
                            enabled = true,
                            activeColorHex = "#10B981",
                            destinations = listOf(
                                BottomNavDestination("services", "الخدمات", "/", "home"),
                                BottomNavDestination("nearby", "الأقرب إليك", "/map", "explore"),
                                BottomNavDestination("bookings", "حجوزاتي", "/orders", "person")
                            )
                        ),
                        packageConfig = PackageConfig(
                            packageName = "com.apkforge.booking",
                            versionName = "1.0.0",
                            versionCode = 1,
                            minSdk = 24,
                            targetSdk = 34
                        ),
                        permissionsConfig = PermissionsConfig(
                            location = true,
                            notifications = true,
                            vibration = true,
                            networkState = true
                        ),
                        bridgeConfig = BridgeAndCloudConfig(
                            jsBridgeEnabled = true,
                            handleExternalIntents = true,
                            deepLinkDomain = "book.apkforge.app",
                            gitHubCiEnabled = true
                        )
                    )
                }
            )
        )
    }
}
