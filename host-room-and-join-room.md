**Now we implement watch party page's controls full advance system with profasonal UI**
Top section is connection status section there will show hotspot is enable or not and local network is active. auto active based on user setup. if no one is active then give intraction how to activate and why need to activate. if connection not disconnect by user then alowse connect in background even app is close or minimize.

After connection status section. next section is room creation and joining feature. create two options "host room" and "join room". now implement host room page or sub-page.

    1.when "host room" is click then show "host room setup page" with auto genarate room id, room name, password filde /non pass option, how many user can join, client control options (like full controls of now playingscreen have, voliume, next video, imogis, Gesture Controls, play/push, etc that related to playing screen), after all this add create room button. after room creation done show curent room data, when click exiting room or resently created room show all data with edit button and advance dynamic qr code system (qr code preview) for scan to join feature, show another option "send join request" for every already conncted devise(same network/ hotspot and wifi) if app is minimize or not already open then send notification directly devise main notification, if app is already open then in app global notification will show, send join links option- if user open this link in any browser in user devise then app quickly open and redirect to app joining page.

    2.  now implement "join room" page. when "join room" section is click then open "join room now" page. in this page fast section will show if any room alredy connect then show this room data(roome id,when create, who create, how many video already played, any video runing or not,etc), then anather section scan qr to join option- when scan button is click then open a sub-page with camera qr scan with scan to join button after click this button show room preview chack (if lots of data is there then use scroll feature to check all data) sub-page with join now button. after jion done show massage for both (host and client) redirect to watch party page.

After "room creation and joining feature" section, create new dynamic section in main page to show data "host room dashbord" related real data if it is host devise. if not, it is client devise then its show "client dashbord" related real data .

    1. if "host room dashbord" then show all accepted user and ban user(in ban user add accepte button, also show all data related to users).
    2. if "client dashbord" then show joined rooms with all data related to rooms, all video list in current playing video folder, etc.

After all this build dynamic mini video player at the top of watch party page ( if host and client connected done and videos already streming) with "streming in full player" button. when user click "streming in full player" button videos play in main player. also add videos stremining popup in app main home page after 'feature video slider in hero section'. also implement streming tag for only when video is streming(host player screen streming tag and client player show live tag).

Remember streming video have all controls avalable in main video player have in room permision so client devise user can use based on
there needs, like (audio track, subtitel, video enhancement, etc in now playingscreen top tool bar have, also all
three dot tool slider page have video streming enable options also) when host devise enable all controls or some
controls when room is created for client devise, if host devise push video client devise still play this video (if
host already give permition in room creation). when host play or streming any video from any folder if this folder
have more videos(5) and host enable folder option, then client player can see all videos in playingscreen tool's
playing queue section, if host not
enable then only current playing video file will only show client playing queue section, if host not enable then only current
playing video file will show.
Also implement playingscreen tool's watch party section when it is a host devise- show rooms list with how many user joined (9,13,17, etc dynamicaly), which user play which video with real time, realtime view data(time of video),etc that make advance and profasonal.

**make "Watch Party" one page** use part or components (sub-pages, contaners, section) and .kt files

**create .kt files for every features page and sub-pages and remember**
!!! IMPORTANT FOR AI: FOLLOW THIS ARCHITECTURE RIGOROUSLY FOR EVERY FILE AND FOLDER CREATED !!!

    **advanced Architecture folder and file name will features wise. files will be lightwate and reuseble**

    _MANDATORY ARCHITECTURE & NAMING CONVENTIONS (CRITICAL)_

    To maintain a professional, industry-grade, and do not havy any files lightweight codebase, this project uses a **Modular Domain-Driven Architecture**. Every file must be self-descriptive and include its feature/folder name in the filename.

    **1. Explicit Feature Naming Mandate**
    Every filename MUST include the name of the feature (folder) it belongs to. This prevents confusion during search and prevents class name collisions.

**check demo of Project watch_party feature Structure**
watch_party/
│
├── core/
│ ├── constants/
│ ├── configuration/
│ ├── helpers/
│ ├── extensions/
│ ├── validators/
│ ├── permissions/
│ ├── exceptions/
│ ├── utilities/
│ └── lifecycle/
│
├── domain/
│ ├── entities/
│ ├── models/
│ ├── enums/
│ ├── repositories/
│ ├── usecases/
│ ├── contracts/
│ ├── policies/
│ └── value_objects/
│
├── data/
│ ├── datasource/
│ ├── repository/
│ ├── cache/
│ ├── datastore/
│ ├── preferences/
│ ├── mapper/
│ └── serializers/
│
├── networking/
│ │
│ ├── discovery/
│ │ ├── wifi/
│ │ ├── hotspot/
│ │ ├── wifi_direct/
│ │ ├── lan/
│ │ ├── mdns/
│ │ ├── scanner/
│ │ └── registry/
│ │
│ ├── connection/
│ │ ├── manager/
│ │ ├── monitor/
│ │ ├── reconnect/
│ │ ├── heartbeat/
│ │ ├── keep_alive/
│ │ └── socket/
│ │
│ ├── protocol/
│ │ ├── tcp/
│ │ ├── udp/
│ │ ├── websocket/
│ │ ├── http/
│ │ └── streaming/
│ │
│ ├── security/
│ │ ├── authentication/
│ │ ├── authorization/
│ │ ├── encryption/
│ │ ├── session/
│ │ └── trusted_device/
│ │
│ └── diagnostics/
│
├── room/
│ ├── create/
│ ├── edit/
│ ├── join/
│ ├── browser/
│ ├── preview/
│ ├── invitation/
│ ├── lifecycle/
│ ├── permissions/
│ ├── roles/
│ ├── qr/
│ ├── deeplink/
│ ├── history/
│ └── settings/
│
├── host/
│ ├── dashboard/
│ ├── room/
│ ├── users/
│ ├── permissions/
│ ├── moderation/
│ ├── playback/
│ ├── streaming/
│ ├── playlist/
│ ├── folder_stream/
│ ├── subtitle/
│ ├── audio/
│ ├── notifications/
│ ├── analytics/
│ ├── queue/
│ └── settings/
│
├── client/
│ ├── dashboard/
│ ├── room/
│ ├── playback/
│ ├── streaming/
│ ├── permissions/
│ ├── queue/
│ ├── notifications/
│ ├── reconnect/
│ ├── history/
│ └── settings/
│
├── streaming/
│ ├── local_server/
│ ├── stream_client/
│ ├── stream_session/
│ ├── stream_quality/
│ ├── adaptive_stream/
│ ├── buffering/
│ ├── bandwidth/
│ ├── playback_sync/
│ ├── subtitle_sync/
│ ├── audio_sync/
│ ├── metadata_sync/
│ ├── queue_sync/
│ ├── folder_sync/
│ ├── latency/
│ ├── recovery/
│ ├── cache/
│ └── diagnostics/
│
├── devices/
│ ├── connected/
│ ├── paired/
│ ├── trusted/
│ ├── blocked/
│ ├── capabilities/
│ ├── permissions/
│ ├── statistics/
│ └── logs/
│
├── notifications/
│ ├── invitation/
│ ├── room/
│ ├── playback/
│ ├── streaming/
│ ├── permissions/
│ ├── global/
│ └── history/
│
├── qr/
│ ├── generator/
│ ├── scanner/
│ ├── validator/
│ ├── expiration/
│ ├── preview/
│ └── history/
│
├── analytics/
│ ├── room/
│ ├── session/
│ ├── playback/
│ ├── bandwidth/
│ ├── latency/
│ ├── buffering/
│ ├── stream_health/
│ └── reports/
│
├── history/
│ ├── sessions/
│ ├── rooms/
│ ├── invitations/
│ ├── playback/
│ └── timeline/
│
├── background/
│ ├── foreground_service/
│ ├── workers/
│ ├── scheduler/
│ ├── reconnect/
│ └── monitoring/
│
├── player_bridge/
│ ├── media3/
│ ├── now_playing/
│ ├── mini_player/
│ ├── queue/
│ ├── subtitle/
│ ├── audio_track/
│ ├── gestures/
│ ├── video_enhancement/
│ └── picture_in_picture/
│
├── ui/
│ │
│ ├── dashboard/
│ ├── connection_status/
│ ├── create_room/
│ ├── edit_room/
│ ├── join_room/
│ ├── room_preview/
│ ├── qr_scanner/
│ ├── qr_preview/
│ ├── host_dashboard/
│ ├── client_dashboard/
│ ├── connected_devices/
│ ├── room_permissions/
│ ├── room_statistics/
│ ├── room_history/
│ ├── invitation/
│ ├── notifications/
│ ├── mini_player/
│ ├── stream_popup/
│ ├── stream_banner/
│ ├── toolbar/
│ ├── dialogs/
│ ├── bottom_sheet/
│ ├── widgets/
│ ├── components/
│ ├── animations/
│ └── theme/
│
├── settings/
│ ├── general/
│ ├── room/
│ ├── streaming/
│ ├── playback/
│ ├── network/
│ ├── security/
│ ├── notifications/
│ ├── qr/
│ └── bandwidth/
│
└── shared/
├── components/
├── dialogs/
├── widgets/
├── adapters/
├── models/
├── extensions/
├── animations/
├── resources/
└── utilities/
