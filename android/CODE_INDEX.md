# 随行原生 Android 客户端 · 代码索引

从 [项目 README](README.md) 了解主流程。下表把当前快照中的源码、页面、脚本与主要配置按目录列出。职责根据代码声明与目录用途概括。

| 文件 | 职责或阅读线索 |
| --- | --- |
| [app/build.gradle.kts](app/build.gradle.kts) | 配置构建、应用启动或环境参数 |
| [app/src/androidTest/java/com/example/let_us_go/ExampleInstrumentedTest.kt](app/src/androidTest/java/com/example/let_us_go/ExampleInstrumentedTest.kt) | 验证功能或回归场景。可从 `ExampleInstrumentedTest` 、 `useAppContext` 开始 |
| [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/assets/provinces.json](app/src/main/assets/provinces.json) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/java/com/example/let_us_go/MainActivity.kt](app/src/main/java/com/example/let_us_go/MainActivity.kt) | Activity 启动。可从 `MainActivity` 、 `onCreate` 、 `AppRoot` 、 `AppNavHost` 开始 |
| [app/src/main/java/com/example/let_us_go/SuiXingApp.kt](app/src/main/java/com/example/let_us_go/SuiXingApp.kt) | 该目录下的业务实现或工具代码。可从 `SuiXingApp` 、 `onCreate` 、 `onTerminate` 、 `onActivityCreated` 开始 |
| [app/src/main/java/com/example/let_us_go/core/base/BaseViewModel.kt](app/src/main/java/com/example/let_us_go/core/base/BaseViewModel.kt) | 定义接口或业务数据结构。可从 `BaseViewModel` 、 `setLoading` 、 `setError` 、 `clearError` 开始 |
| [app/src/main/java/com/example/let_us_go/core/bus/AppEventBus.kt](app/src/main/java/com/example/let_us_go/core/bus/AppEventBus.kt) | 该目录下的业务实现或工具代码。可从 `PostCreated` 、 `CommentChanged` 、 `LoginStateChanged` 、 `FontScaleChanged` 开始 |
| [app/src/main/java/com/example/let_us_go/core/float/ExoPlayerManager.kt](app/src/main/java/com/example/let_us_go/core/float/ExoPlayerManager.kt) | 该目录下的业务实现或工具代码。可从 `getOrCreate` 、 `release` 、 `pause` 、 `forceRelease` 开始 |
| [app/src/main/java/com/example/let_us_go/core/float/FloatChatManager.kt](app/src/main/java/com/example/let_us_go/core/float/FloatChatManager.kt) | 该目录下的业务实现或工具代码。可从 `class` 、 `startChat` 、 `showDialog` 、 `dismissDialog` 开始 |
| [app/src/main/java/com/example/let_us_go/core/float/FloatingWindow.kt](app/src/main/java/com/example/let_us_go/core/float/FloatingWindow.kt) | 该目录下的业务实现或工具代码。可从 `FloatingWindow` 、 `handleFloatTap` 、 `DialogPanel` 、 `WaveAnimation` 开始 |
| [app/src/main/java/com/example/let_us_go/core/location/AndroidLocationProvider.kt](app/src/main/java/com/example/let_us_go/core/location/AndroidLocationProvider.kt) | 该目录下的业务实现或工具代码。可从 `ProviderLocation` 、 `AndroidLocationProvider` 、 `start` 、 `register` 开始 |
| [app/src/main/java/com/example/let_us_go/core/playback/DigitalHumanPlayer.kt](app/src/main/java/com/example/let_us_go/core/playback/DigitalHumanPlayer.kt) | 该目录下的业务实现或工具代码。可从 `DigitalHumanPlayer` 、 `onPlaybackStateChanged` 、 `onIsPlayingChanged` 、 `onPlayerError` 开始 |
| [app/src/main/java/com/example/let_us_go/core/playback/DigitalHumanPlayerHolder.kt](app/src/main/java/com/example/let_us_go/core/playback/DigitalHumanPlayerHolder.kt) | 该目录下的业务实现或工具代码。可从 `DigitalHumanPlayerHolder` 、 `onPlaybackStateChanged` 、 `onIsPlayingChanged` 、 `onPlayerError` 开始 |
| [app/src/main/java/com/example/let_us_go/core/service/WakeUpController.kt](app/src/main/java/com/example/let_us_go/core/service/WakeUpController.kt) | 实现服务层逻辑或外部资源调用。可从 `setEnabled` 、 `setLastKeyword` 、 `WakeUpController` 、 `ensurePermissions` 开始 |
| [app/src/main/java/com/example/let_us_go/core/service/WakeUpService.kt](app/src/main/java/com/example/let_us_go/core/service/WakeUpService.kt) | 实现服务层逻辑或外部资源调用。可从 `WakeUpService` 、 `onBind` 、 `onCreate` 、 `onStartCommand` 开始 |
| [app/src/main/java/com/example/let_us_go/data/api/AdminApi.kt](app/src/main/java/com/example/let_us_go/data/api/AdminApi.kt) | 封装接口调用、连接或数据解析 |
| [app/src/main/java/com/example/let_us_go/data/api/AsrApi.kt](app/src/main/java/com/example/let_us_go/data/api/AsrApi.kt) | 封装接口调用、连接或数据解析。可从 `AsrApi` 、 `AsrStatus` 、 `AsrPreloadBody` 、 `AsrModel` 开始 |
| [app/src/main/java/com/example/let_us_go/data/api/AuthApi.kt](app/src/main/java/com/example/let_us_go/data/api/AuthApi.kt) | 封装接口调用、连接或数据解析。可从 `AuthApi` 开始 |
| [app/src/main/java/com/example/let_us_go/data/api/CheckinApi.kt](app/src/main/java/com/example/let_us_go/data/api/CheckinApi.kt) | 封装接口调用、连接或数据解析。可从 `CheckinApi` 、 `CheckinRequest` 、 `CreateCheckinBody` 开始 |
| [app/src/main/java/com/example/let_us_go/data/api/CircleApi.kt](app/src/main/java/com/example/let_us_go/data/api/CircleApi.kt) | 封装接口调用、连接或数据解析。可从 `CircleApi` 、 `CreatePostResult` 、 `CommentListData` 、 `CreatePostBody` 开始 |
| [app/src/main/java/com/example/let_us_go/data/api/DeepSeekApi.kt](app/src/main/java/com/example/let_us_go/data/api/DeepSeekApi.kt) | 封装接口调用、连接或数据解析。可从 `decryptKey` 、 `encryptKey` 、 `parseResponse` 开始 |
| [app/src/main/java/com/example/let_us_go/data/api/DigitalHumanApi.kt](app/src/main/java/com/example/let_us_go/data/api/DigitalHumanApi.kt) | 封装接口调用、连接或数据解析。可从 `DigitalHumanApi` 开始 |
| [app/src/main/java/com/example/let_us_go/data/api/GuideApi.kt](app/src/main/java/com/example/let_us_go/data/api/GuideApi.kt) | 封装接口调用、连接或数据解析。可从 `GuideApi` 、 `ListData` 、 `PoiListResponse` 、 `PoiRaw` 开始 |
| [app/src/main/java/com/example/let_us_go/data/api/KnowledgeGraphApi.kt](app/src/main/java/com/example/let_us_go/data/api/KnowledgeGraphApi.kt) | 封装接口调用、连接或数据解析。可从 `KnowledgeGraphApi` 开始 |
| [app/src/main/java/com/example/let_us_go/data/api/RealtimeExplainApi.kt](app/src/main/java/com/example/let_us_go/data/api/RealtimeExplainApi.kt) | 封装接口调用、连接或数据解析。可从 `RealtimeExplainApi` 、 `RealtimeExplainHistoryPage` 、 `RealtimeExplainHistoryBody` 开始 |
| [app/src/main/java/com/example/let_us_go/data/api/SuggestionApi.kt](app/src/main/java/com/example/let_us_go/data/api/SuggestionApi.kt) | 封装接口调用、连接或数据解析。可从 `SuggestionApi` 、 `CreateSuggestionBody` 开始 |
| [app/src/main/java/com/example/let_us_go/data/api/UploadApi.kt](app/src/main/java/com/example/let_us_go/data/api/UploadApi.kt) | 封装接口调用、连接或数据解析。可从 `UploadApi` 、 `UploadImagesResult` 、 `UploadImageResponse` 、 `UploadVideoResponse` 开始 |
| [app/src/main/java/com/example/let_us_go/data/api/UserProfileApi.kt](app/src/main/java/com/example/let_us_go/data/api/UserProfileApi.kt) | 封装接口调用、连接或数据解析。可从 `UserProfileApi` 、 `UserProfileData` 、 `UpdateProfileRequest` 、 `ChangePasswordRequest` 开始 |
| [app/src/main/java/com/example/let_us_go/data/api/WeatherApi.kt](app/src/main/java/com/example/let_us_go/data/api/WeatherApi.kt) | 封装接口调用、连接或数据解析。可从 `WeatherApi` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/Avatar.kt](app/src/main/java/com/example/let_us_go/data/model/Avatar.kt) | 定义接口或业务数据结构。可从 `AvatarSession` 、 `AvatarChatHistoryPage` 、 `AvatarChatHistoryItem` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/AvatarDriveAudioRequest.kt](app/src/main/java/com/example/let_us_go/data/model/AvatarDriveAudioRequest.kt) | 定义接口或业务数据结构。可从 `AvatarDriveAudioUrlRequest` 、 `AvatarDriveAudioUrlResponse` 、 `AvatarDriveAudioProxyRequest` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/AvatarRequests.kt](app/src/main/java/com/example/let_us_go/data/model/AvatarRequests.kt) | 定义接口或业务数据结构。可从 `AvatarDriveTextRequest` 、 `AvatarPingRequest` 、 `AvatarStopRequest` 、 `AvatarConfig` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/AvatarStartRequest.kt](app/src/main/java/com/example/let_us_go/data/model/AvatarStartRequest.kt) | 定义接口或业务数据结构。可从 `AvatarStartRequest` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/Chat.kt](app/src/main/java/com/example/let_us_go/data/model/Chat.kt) | 定义接口或业务数据结构。可从 `ChatRequest` 、 `GeoPoint` 、 `ChatMessage` 、 `ChatResponse` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/CheckinRecord.kt](app/src/main/java/com/example/let_us_go/data/model/CheckinRecord.kt) | 定义接口或业务数据结构。可从 `CheckinRecord` 、 `ProvinceCheckin` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/Guide.kt](app/src/main/java/com/example/let_us_go/data/model/Guide.kt) | 定义接口或业务数据结构。可从 `ScenicSpot` 、 `Event` 、 `Food` 、 `NearbyShop` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/ImageRecognitionResponse.kt](app/src/main/java/com/example/let_us_go/data/model/ImageRecognitionResponse.kt) | 定义接口或业务数据结构。可从 `ImageRecognitionResponse` 、 `answerText` 、 `speechText` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/Itinerary.kt](app/src/main/java/com/example/let_us_go/data/model/Itinerary.kt) | 定义接口或业务数据结构。可从 `Itinerary` 、 `ItineraryItem` 、 `ItineraryParseResult` 、 `HotelListData` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/KG.kt](app/src/main/java/com/example/let_us_go/data/model/KG.kt) | 定义接口或业务数据结构。可从 `KGGraphData` 、 `KGNode` 、 `KGEdge` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/Post.kt](app/src/main/java/com/example/let_us_go/data/model/Post.kt) | 定义接口或业务数据结构。可从 `Post` 、 `PostImage` 、 `PostVideo` 、 `Comment` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/RealtimeExplainHistory.kt](app/src/main/java/com/example/let_us_go/data/model/RealtimeExplainHistory.kt) | 定义接口或业务数据结构。可从 `RealtimeExplainHistory` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/RoutePlanRequest.kt](app/src/main/java/com/example/let_us_go/data/model/RoutePlanRequest.kt) | 定义接口或业务数据结构。可从 `RoutePlanRequest` 、 `LocationPoint` 、 `RoutePlanResponse` 、 `RoutePlanData` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/Suggestion.kt](app/src/main/java/com/example/let_us_go/data/model/Suggestion.kt) | 定义接口或业务数据结构。可从 `Suggestion` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/UploadedFile.kt](app/src/main/java/com/example/let_us_go/data/model/UploadedFile.kt) | 定义接口或业务数据结构。可从 `UploadedFile` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/User.kt](app/src/main/java/com/example/let_us_go/data/model/User.kt) | 定义接口或业务数据结构。可从 `User` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/UserAdapterFactory.kt](app/src/main/java/com/example/let_us_go/data/model/UserAdapterFactory.kt) | 定义接口或业务数据结构。可从 `create` 、 `fromJson` 、 `toJson` 、 `readNullString` 开始 |
| [app/src/main/java/com/example/let_us_go/data/model/Weather.kt](app/src/main/java/com/example/let_us_go/data/model/Weather.kt) | 定义接口或业务数据结构。可从 `WeatherCurrent` 、 `WeatherForecastDaily` 、 `WeatherCast` 开始 |
| [app/src/main/java/com/example/let_us_go/data/network/ApiResult.kt](app/src/main/java/com/example/let_us_go/data/network/ApiResult.kt) | 封装接口调用、连接或数据解析。可从 `ApiResult` 、 `EmptyData` 开始 |
| [app/src/main/java/com/example/let_us_go/data/network/AuthInterceptor.kt](app/src/main/java/com/example/let_us_go/data/network/AuthInterceptor.kt) | 封装接口调用、连接或数据解析。可从 `AuthInterceptor` 、 `intercept` 开始 |
| [app/src/main/java/com/example/let_us_go/data/network/MoshiLenientAdapters.kt](app/src/main/java/com/example/let_us_go/data/network/MoshiLenientAdapters.kt) | 封装接口调用、连接或数据解析。可从 `LenientStringAdapter` 、 `fromJson` 、 `toJson` 、 `LenientIntAdapter` 开始 |
| [app/src/main/java/com/example/let_us_go/data/network/NetworkModule.kt](app/src/main/java/com/example/let_us_go/data/network/NetworkModule.kt) | 封装接口调用、连接或数据解析。可从 `init` 、 `ensureSlash` 、 `moshi` 开始 |
| [app/src/main/java/com/example/let_us_go/data/prefs/ConversationStore.kt](app/src/main/java/com/example/let_us_go/data/prefs/ConversationStore.kt) | 管理本地状态、偏好或持久化。可从 `startNewRound` 、 `appendMessage` 、 `getCurrentMessages` 、 `getMessagesBySource` 开始 |
| [app/src/main/java/com/example/let_us_go/data/prefs/DhConversationStore.kt](app/src/main/java/com/example/let_us_go/data/prefs/DhConversationStore.kt) | 管理本地状态、偏好或持久化。可从 `ConversationRecord` 、 `appendMessage` 、 `appendMessages` 、 `write` 开始 |
| [app/src/main/java/com/example/let_us_go/data/prefs/ItineraryStore.kt](app/src/main/java/com/example/let_us_go/data/prefs/ItineraryStore.kt) | 管理本地状态、偏好或持久化。可从 `load` 、 `save` 、 `addItem` 、 `removeItem` 开始 |
| [app/src/main/java/com/example/let_us_go/data/prefs/LoginStateFile.kt](app/src/main/java/com/example/let_us_go/data/prefs/LoginStateFile.kt) | 管理本地状态、偏好或持久化。可从 `read` 、 `readUserId` 、 `write` 、 `clear` 开始 |
| [app/src/main/java/com/example/let_us_go/data/prefs/SpotImageCache.kt](app/src/main/java/com/example/let_us_go/data/prefs/SpotImageCache.kt) | 管理本地状态、偏好或持久化。可从 `refresh` 、 `forceRefresh` 、 `getImage` 开始 |
| [app/src/main/java/com/example/let_us_go/data/prefs/UserPrefs.kt](app/src/main/java/com/example/let_us_go/data/prefs/UserPrefs.kt) | 管理本地状态、偏好或持久化。可从 `init` 、 `tokenSync` 、 `tokenFlow` 、 `isLoggedIn` 开始 |
| [app/src/main/java/com/example/let_us_go/data/ws/AsrWebSocket.kt](app/src/main/java/com/example/let_us_go/data/ws/AsrWebSocket.kt) | 封装接口调用、连接或数据解析。可从 `AsrWebSocket` 、 `connectToAsr` 、 `sendConfig` 、 `sendAudio` 开始 |
| [app/src/main/java/com/example/let_us_go/data/ws/BaseWebSocket.kt](app/src/main/java/com/example/let_us_go/data/ws/BaseWebSocket.kt) | 封装接口调用、连接或数据解析。可从 `BaseWebSocket` 、 `class` 、 `open` 、 `connect` 开始 |
| [app/src/main/java/com/example/let_us_go/data/ws/XunfeiAvatarWebSocket.kt](app/src/main/java/com/example/let_us_go/data/ws/XunfeiAvatarWebSocket.kt) | 封装接口调用、连接或数据解析。可从 `XunfeiAvatarWebSocket` 、 `connect` 、 `buildAuthorizedUrl` 、 `sendStart` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/components/HotelCard.kt](app/src/main/java/com/example/let_us_go/ui/components/HotelCard.kt) | 实现页面、组件或用户交互。可从 `HotelCardList` 、 `HotelCardItem` 、 `CardActionButton` 、 `AddToTripButton` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/components/RemoteImage.kt](app/src/main/java/com/example/let_us_go/ui/components/RemoteImage.kt) | 实现页面、组件或用户交互。可从 `RemoteImage` 、 `PlaceholderBox` 、 `AvatarImage` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/nav/AppBottomBar.kt](app/src/main/java/com/example/let_us_go/ui/nav/AppBottomBar.kt) | 该目录下的业务实现或工具代码。可从 `AppBottomBar` 、 `TabItem` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/nav/NavExt.kt](app/src/main/java/com/example/let_us_go/ui/nav/NavExt.kt) | 该目录下的业务实现或工具代码。可从 `NavController` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/nav/Route.kt](app/src/main/java/com/example/let_us_go/ui/nav/Route.kt) | 该目录下的业务实现或工具代码 |
| [app/src/main/java/com/example/let_us_go/ui/pages/aging/AgingScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/aging/AgingScreen.kt) | 实现页面、组件或用户交互。可从 `AgingScreen` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/ai_chat/AiChatScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/ai_chat/AiChatScreen.kt) | 实现页面、组件或用户交互。可从 `AiChatScreen` 、 `ChatBubble` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/ai_chat/AiChatViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/ai_chat/AiChatViewModel.kt) | 定义接口或业务数据结构。可从 `AiChatViewModel` 、 `loadHistory` 、 `ensureWelcome` 、 `send` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/checkin/CheckinScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/checkin/CheckinScreen.kt) | 实现页面、组件或用户交互。可从 `CheckinScreen` 、 `PhotoSpotCard` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/checkin/CheckinViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/checkin/CheckinViewModel.kt) | 定义接口或业务数据结构。可从 `CheckinViewModel` 、 `load` 、 `checkIn` 、 `autoCheckinByLocation` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/checkin/PhotoSpotsViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/checkin/PhotoSpotsViewModel.kt) | 定义接口或业务数据结构。可从 `PhotoSpotsViewModel` 、 `load` 、 `loadMore` 、 `refresh` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/digital_human/DigitalHumanScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/digital_human/DigitalHumanScreen.kt) | 实现页面、组件或用户交互。可从 `rememberIsKeyboardVisible` 、 `DigitalHumanScreen` 、 `launchCameraCapture` 、 `onTakePhoto` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/digital_human/DigitalHumanViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/digital_human/DigitalHumanViewModel.kt) | 定义接口或业务数据结构。可从 `DigitalHumanViewModel` 、 `class` 、 `toggeDetail` 、 `startMoodSimulation` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/digital_human/ImageRecognitionOverlay.kt](app/src/main/java/com/example/let_us_go/ui/pages/digital_human/ImageRecognitionOverlay.kt) | 实现页面、组件或用户交互。可从 `ImageRecognitionOverlay` 、 `speakAll` 、 `startPlayback` 、 `pausePlayback` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/digital_stream/DigitalStreamScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/digital_stream/DigitalStreamScreen.kt) | 实现页面、组件或用户交互。可从 `DigitalStreamScreen` 、 `StreamRow` 、 `StreamInfo` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/digital_stream/DigitalStreamViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/digital_stream/DigitalStreamViewModel.kt) | 定义接口或业务数据结构。可从 `DigitalStreamViewModel` 、 `startStream` 、 `selectStream` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/events/EventsScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/events/EventsScreen.kt) | 实现页面、组件或用户交互。可从 `EventsScreen` 、 `EventCard` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/events/EventsViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/events/EventsViewModel.kt) | 定义接口或业务数据结构。可从 `EventsViewModel` 、 `load` 、 `loadMore` 、 `refresh` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/food/FoodScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/food/FoodScreen.kt) | 实现页面、组件或用户交互。可从 `FoodScreen` 、 `FoodRow` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/food/FoodViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/food/FoodViewModel.kt) | 定义接口或业务数据结构。可从 `FoodViewModel` 、 `load` 、 `loadMore` 、 `refresh` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/guide/GuideDetailScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/guide/GuideDetailScreen.kt) | 实现页面、组件或用户交互。可从 `GuideDetailScreen` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/guide/GuideScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/guide/GuideScreen.kt) | 实现页面、组件或用户交互。可从 `GuideScreen` 、 `GuideRow` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/guide/GuideTipHolder.kt](app/src/main/java/com/example/let_us_go/ui/pages/guide/GuideTipHolder.kt) | 实现页面、组件或用户交互 |
| [app/src/main/java/com/example/let_us_go/ui/pages/guide/GuideViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/guide/GuideViewModel.kt) | 定义接口或业务数据结构。可从 `GuideViewModel` 、 `load` 、 `loadMore` 、 `refresh` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/help/HelpCenterScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/help/HelpCenterScreen.kt) | 实现页面、组件或用户交互。可从 `HelpCenterScreen` 、 `HelpSectionCard` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/help/HelpData.kt](app/src/main/java/com/example/let_us_go/ui/pages/help/HelpData.kt) | 实现页面、组件或用户交互。可从 `HelpSection` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/home/HomeScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/home/HomeScreen.kt) | 实现页面、组件或用户交互。可从 `HomeScreen` 、 `mapWeatherIcon` 、 `HeroBanner` 、 `FunctionGrid` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/home/HomeViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/home/HomeViewModel.kt) | 定义接口或业务数据结构。可从 `HomeViewModel` 、 `loadAll` 、 `loadWeather` 、 `loadHotSpots` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/hotspots/HotspotsScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/hotspots/HotspotsScreen.kt) | 实现页面、组件或用户交互。可从 `HotspotsScreen` 、 `TopBar` 、 `SpotRow` 、 `SpotDetailPanel` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/hotspots/HotspotsViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/hotspots/HotspotsViewModel.kt) | 定义接口或业务数据结构。可从 `HotspotsViewModel` 、 `setFilter` 、 `load` 、 `getImageFor` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/index/IndexScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/index/IndexScreen.kt) | 实现页面、组件或用户交互。可从 `IndexScreen` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/itinerary/ItineraryScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/itinerary/ItineraryScreen.kt) | 实现页面、组件或用户交互。可从 `ItineraryScreen` 、 `AiPlanDialog` 、 `ItineraryRow` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/itinerary/ItineraryViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/itinerary/ItineraryViewModel.kt) | 定义接口或业务数据结构。可从 `ItineraryViewModel` 、 `loadFromStore` 、 `addFromBooking` 、 `planByAi` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/live_guide/LiveGuideFlowScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/live_guide/LiveGuideFlowScreen.kt) | 实现页面、组件或用户交互。可从 `LiveGuideFlowScreen` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/live_guide/LiveGuidePhotoScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/live_guide/LiveGuidePhotoScreen.kt) | 实现页面、组件或用户交互。可从 `LiveGuidePhotoScreen` 、 `launchCameraCapture` 、 `onTakePhoto` 、 `OptionCard` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/live_guide/LiveGuideScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/live_guide/LiveGuideScreen.kt) | 实现页面、组件或用户交互。可从 `LiveGuideScreen` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/live_guide/LiveGuideViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/live_guide/LiveGuideViewModel.kt) | 定义接口或业务数据结构。可从 `LiveGuideViewModel` 、 `recognizeImage` 、 `clearResult` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/login/LoginScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/login/LoginScreen.kt) | 实现页面、组件或用户交互。可从 `LoginScreen` 、 `TopNavBar` 、 `LogoSection` 、 `FormCard` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/login/LoginViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/login/LoginViewModel.kt) | 定义接口或业务数据结构。可从 `LoginViewModel` 、 `login` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/map/MapScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/map/MapScreen.kt) | 实现页面、组件或用户交互。可从 `poiTypeLabel` 、 `TtsController` 、 `speak` 、 `stop` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/map/MapViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/map/MapViewModel.kt) | 定义接口或业务数据结构。可从 `MapViewModel` 、 `class` 、 `bindAmap` 、 `loadPois` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/mine/AvatarChatDetailViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/mine/AvatarChatDetailViewModel.kt) | 定义接口或业务数据结构。可从 `AvatarChatDetailViewModel` 、 `load` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/mine/AvatarChatHistoryViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/mine/AvatarChatHistoryViewModel.kt) | 定义接口或业务数据结构。可从 `AvatarChatHistoryViewModel` 、 `load` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/mine/ChinaMapScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/mine/ChinaMapScreen.kt) | 实现页面、组件或用户交互。可从 `ChinaMapScreen` 、 `drawLitProvinces` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/mine/DhHistoryViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/mine/DhHistoryViewModel.kt) | 定义接口或业务数据结构。可从 `DhHistoryViewModel` 、 `load` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/mine/FeedbackViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/mine/FeedbackViewModel.kt) | 定义接口或业务数据结构。可从 `FeedbackViewModel` 、 `load` 、 `submit` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/mine/MineScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/mine/MineScreen.kt) | 实现页面、组件或用户交互。可从 `MineScreen` 、 `UserHeaderCard` 、 `ProvinceCityCard` 、 `QuickServicesGrid` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/mine/MineSubScreens.kt](app/src/main/java/com/example/let_us_go/ui/pages/mine/MineSubScreens.kt) | 实现页面、组件或用户交互。可从 `toAbs` 、 `SubPageScaffold` 、 `LoadingOrEmpty` 、 `MyPostsScreen` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/mine/MineViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/mine/MineViewModel.kt) | 定义接口或业务数据结构。可从 `MineViewModel` 、 `loadAll` 、 `loadProfile` 、 `loadProvinceCount` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/mine/MyCommentsViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/mine/MyCommentsViewModel.kt) | 定义接口或业务数据结构。可从 `MyCommentsViewModel` 、 `load` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/mine/MyLikesViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/mine/MyLikesViewModel.kt) | 定义接口或业务数据结构。可从 `MyLikesViewModel` 、 `load` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/mine/MyPostsViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/mine/MyPostsViewModel.kt) | 定义接口或业务数据结构。可从 `MyPostsViewModel` 、 `load` 、 `deletePost` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/mine/ProfileEditScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/mine/ProfileEditScreen.kt) | 实现页面、组件或用户交互。可从 `ProfileEditScreen` 、 `TopNavBar` 、 `ProfileAvatar` 、 `ProfileInfoCard` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/mine/ProfileEditViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/mine/ProfileEditViewModel.kt) | 定义接口或业务数据结构。可从 `ProfileEditViewModel` 、 `loadProfile` 、 `updateProfile` 、 `changePassword` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/moments/MomentsScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/moments/MomentsScreen.kt) | 实现页面、组件或用户交互。可从 `MomentsScreen` 、 `MomentsHeader` 、 `HeroBanner` 、 `FeedCard` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/moments/MomentsViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/moments/MomentsViewModel.kt) | 定义接口或业务数据结构。可从 `MomentsViewModel` 、 `loadAll` 、 `loadPosts` 、 `loadMore` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/nearby/NearbyScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/nearby/NearbyScreen.kt) | 实现页面、组件或用户交互。可从 `NearbyScreen` 、 `ShopRow` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/nearby/NearbyViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/nearby/NearbyViewModel.kt) | 定义接口或业务数据结构。可从 `NearbyViewModel` 、 `load` 、 `loadMore` 、 `refresh` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/placeholder/PlaceholderScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/placeholder/PlaceholderScreen.kt) | 实现页面、组件或用户交互。可从 `PlaceholderScreen` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/register/RegisterScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/register/RegisterScreen.kt) | 实现页面、组件或用户交互。可从 `RegisterScreen` 、 `TopNavBar` 、 `LogoSection` 、 `FormCard` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/register/RegisterViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/register/RegisterViewModel.kt) | 定义接口或业务数据结构。可从 `RegisterViewModel` 、 `register` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/scenic/ScenicScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/scenic/ScenicScreen.kt) | 实现页面、组件或用户交互。可从 `ScenicScreen` 、 `KGNode` 、 `KnowledgeGraph` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/scenic/ScenicViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/scenic/ScenicViewModel.kt) | 定义接口或业务数据结构。可从 `ScenicViewModel` 、 `load` 、 `filterByTag` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/search/SearchScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/search/SearchScreen.kt) | 实现页面、组件或用户交互。可从 `SearchScreen` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/search/SearchViewModel.kt](app/src/main/java/com/example/let_us_go/ui/pages/search/SearchViewModel.kt) | 定义接口或业务数据结构。可从 `SearchViewModel` 、 `search` 、 `clearHistory` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/pages/splash/SplashScreen.kt](app/src/main/java/com/example/let_us_go/ui/pages/splash/SplashScreen.kt) | 实现页面、组件或用户交互。可从 `SplashScreen` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/theme/Color.kt](app/src/main/java/com/example/let_us_go/ui/theme/Color.kt) | 该目录下的业务实现或工具代码 |
| [app/src/main/java/com/example/let_us_go/ui/theme/Theme.kt](app/src/main/java/com/example/let_us_go/ui/theme/Theme.kt) | 该目录下的业务实现或工具代码。可从 `scaledTypography` 、 `sp` 、 `SuiXingTheme` 、 `Let_us_goTheme` 开始 |
| [app/src/main/java/com/example/let_us_go/ui/theme/Type.kt](app/src/main/java/com/example/let_us_go/ui/theme/Type.kt) | 该目录下的业务实现或工具代码 |
| [app/src/main/java/com/example/let_us_go/util/Crypto.kt](app/src/main/java/com/example/let_us_go/util/Crypto.kt) | 该目录下的业务实现或工具代码。可从 `hmacSha256` 、 `base64Encode` 、 `base64Decode` 、 `generateXunfeiAuth` 开始 |
| [app/src/main/java/com/example/let_us_go/util/Permissions.kt](app/src/main/java/com/example/let_us_go/util/Permissions.kt) | 该目录下的业务实现或工具代码。可从 `rememberImagePicker` 、 `rememberCameraLauncher` 、 `rememberRecordAudioPermission` 、 `rememberCameraPermission` 开始 |
| [app/src/main/res/drawable/bg_chat_ai_bubble.xml](app/src/main/res/drawable/bg_chat_ai_bubble.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/bg_chat_panel.xml](app/src/main/res/drawable/bg_chat_panel.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/bg_chat_user_bubble.xml](app/src/main/res/drawable/bg_chat_user_bubble.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/bg_float_avatar.xml](app/src/main/res/drawable/bg_float_avatar.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/bg_float_close.xml](app/src/main/res/drawable/bg_float_close.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/bg_float_dh.xml](app/src/main/res/drawable/bg_float_dh.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/bg_float_tip.xml](app/src/main/res/drawable/bg_float_tip.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/bg_recording_indicator.xml](app/src/main/res/drawable/bg_recording_indicator.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/circle_background.xml](app/src/main/res/drawable/circle_background.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/circle_red.xml](app/src/main/res/drawable/circle_red.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_digital_human.xml](app/src/main/res/drawable/ic_digital_human.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_launcher_background.xml](app/src/main/res/drawable/ic_launcher_background.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_launcher_foreground.xml](app/src/main/res/drawable/ic_launcher_foreground.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_tool_attraction.xml](app/src/main/res/drawable/ic_tool_attraction.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_tool_digital.xml](app/src/main/res/drawable/ic_tool_digital.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_tool_entrance.xml](app/src/main/res/drawable/ic_tool_entrance.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_tool_exit.xml](app/src/main/res/drawable/ic_tool_exit.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_tool_food.xml](app/src/main/res/drawable/ic_tool_food.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_tool_medical.xml](app/src/main/res/drawable/ic_tool_medical.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_tool_rest.xml](app/src/main/res/drawable/ic_tool_rest.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_tool_service.xml](app/src/main/res/drawable/ic_tool_service.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_tool_shop.xml](app/src/main/res/drawable/ic_tool_shop.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_tool_toilet.xml](app/src/main/res/drawable/ic_tool_toilet.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/layout/layout_float_chat.xml](app/src/main/res/layout/layout_float_chat.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/layout/layout_float_dh.xml](app/src/main/res/layout/layout_float_dh.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/layout/layout_float_new.xml](app/src/main/res/layout/layout_float_new.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml](app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml](app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/values/colors.xml](app/src/main/res/values/colors.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/values/strings.xml](app/src/main/res/values/strings.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/values/themes.xml](app/src/main/res/values/themes.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/xml/backup_rules.xml](app/src/main/res/xml/backup_rules.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/xml/data_extraction_rules.xml](app/src/main/res/xml/data_extraction_rules.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/xml/file_paths.xml](app/src/main/res/xml/file_paths.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/xml/network_security_config.xml](app/src/main/res/xml/network_security_config.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/test/java/com/example/let_us_go/ExampleUnitTest.kt](app/src/test/java/com/example/let_us_go/ExampleUnitTest.kt) | 验证功能或回归场景。可从 `ExampleUnitTest` 、 `addition_isCorrect` 开始 |
| [build.gradle.kts](build.gradle.kts) | 配置构建、应用启动或环境参数 |
| [gradlew.bat](gradlew.bat) | 提供运行、构建或部署辅助 |
| [settings.gradle.kts](settings.gradle.kts) | 配置构建、应用启动或环境参数 |

本页列出 171 个可阅读的项目文件，以及 0 个第三方或生成文件。
