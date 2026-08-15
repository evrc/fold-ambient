# Samsung Galaxy Z Fold6 Device Findings

These findings were observed on a physical Samsung Galaxy Z Fold6. These results come from testing the real device, not an emulator.

## Cover Display Observations

### Portrait

```text
window = 968x2376
displayId = 0
rotation = ROTATION_0
TYPE_HINGE_ANGLE available = true
FoldingFeature = none
```

### Landscape

```text
window = 2376x968
displayId = 0
rotation = ROTATION_90
TYPE_HINGE_ANGLE available = true
```

The raw hinge sensor continued reporting while the application remained on the cover display.

Observed values included:

```text
closed: approximately 0 degrees
partially open: 90 degrees
```

At 90 degrees while still on the cover display:

```text
FoldingFeature = none
```

Therefore WindowManager `FoldingFeature` must not be assumed to describe cover-screen tent/wedge posture.

## Inner Display Observations

### Half-Open Landscape

```text
window = 2160x1856
displayId = 0
hinge angle = 90 degrees
```

WindowManager reported:

```text
state = HALF_OPENED
orientation = HORIZONTAL
isSeparating = true
```

### Flat

```text
window = 2160x1856 or 1856x2160 depending on rotation
displayId = 0
hinge angle = 180 degrees
```

WindowManager reported:

```text
state = FLAT
isSeparating = false
```

## Important Conclusions

1. `Sensor.TYPE_HINGE_ANGLE` is exposed to a normal application on this Fold6.
2. The hinge angle sensor remains usable while the application is running on the cover display.
3. `displayId` cannot distinguish the cover and inner displays: both were observed as `displayId = 0`.
4. `WindowLayoutInfo` / `FoldingFeature` is useful for the inner display but is not a reliable primary indicator of cover-screen tent/wedge posture.
5. Cover and inner displays can currently be distinguished very clearly by window geometry.

Observed aspect ratios are approximately:

```text
cover: 2376 / 968 ~= 2.45
inner: 2160 / 1856 ~= 1.16
```

Future posture detection should therefore investigate a combination of:

- raw hinge angle;
- current window geometry / aspect ratio;
- orientation;
- potentially charging or motion state as policy inputs.

Do not hardcode the exact observed pixel dimensions as the permanent cover-screen detector. Resolution, display scaling, or other devices may change them.

Do not yet hardcode an ambient hinge-angle range such as 20-150 degrees. We need additional real-world use before choosing appropriate thresholds.

Do not implement automatic ambient-mode activation yet. These findings are preserved for the later automatic-activation phase.
