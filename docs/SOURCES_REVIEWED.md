# Sources reviewed for this package

- Hammerhead karoo-ext repository and README
- Hammerhead karoo-ext API documentation for KarooExtension, DataTypeImpl, KarooSystemService, DataType.Type/Field, OnStreamState, StreamState, UserProfile, ViewConfig, UpdateGraphicConfig, InRideAlert and PlayBeepPattern
- Hammerhead Extensions FAQ
- Android official adb documentation
- Android official sdkmanager / command-line tools documentation
- Android Studio official download and SDK setup pages

Key implementation decisions:

- Use `karoo-ext` v1.1.9.
- Register one graphical `DataType` with `graphical="true"`.
- Render the field through `RemoteViews` in `DataTypeImpl.startView`.
- Read FTP through `UserProfile.ftp`, with a manual fallback.
- Stream 3s power through `OnStreamState.StartStreaming(DataType.Type.SMOOTHED_3S_AVERAGE_POWER)` and fallback to instant `DataType.Type.POWER`.
- Stream cadence through `DataType.Type.CADENCE` for K3 low cadence warnings.
- Use `InRideAlert` and `PlayBeepPattern` on segment changes.


Additional v1.1.3 checks:
- karoo-ext DataType.Type.SPEED: current speed stream.
- karoo-ext DataType.Type.HEART_RATE: current HR stream.
- karoo-ext DataType.Type.CLOCK_TIME exists; app renders local Android clock directly for reliable cockpit time.
- karoo-ext StreamState.Streaming carries DataPoint values maps.
