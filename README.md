# SnappyRulerSet

SnappyRulerSet is an Android drawing application focused on precise and intuitive geometric constructions. It includes core virtual geometry tools such as a draggable, rotatable ruler with snapping to common angles and existing line points; set squares with 45° and 30°–60° variants; a protractor that measures and snaps angles with high accuracy; and optionally a compass for drawing arcs and circles.

The app is implemented using **Kotlin and Jetpack Compose**, with modular architecture separating rendering, interaction, and data persistence layers. Snapping behavior adapts dynamically with zoom level and prioritizes nearest snap targets with clear visual cues. The app is **offline-first**, requiring no network connectivity.

## Major Highlights

- MVVM Architecture
- Offline First
- Kotlin
- Flows
- Stateflow
- Compose UI

## Features Implemented

- Grid Snap Toggle - On/Off
- Common Angle Snapping 
- Free Hand Drawing
- Compass Tool
- Protractor Tool
- Set Square Tool
- Export PNG/JPG
- Grid Resizing
- Undo and Redo Support
- Precision Hud for selected Tool
  
## Documentation References:
- Jetpack Compose Graphics Overview
```
https://developer.android.com/develop/ui/compose/graphics/draw/overview
```
- Drawing Shapes
```
https://developer.android.com/develop/ui/compose/graphics/draw/shapes
```
- Graphic Modifiers
```
https://developer.android.com/develop/ui/compose/graphics/draw/modifiers
```
- Multi Touch Input
```
https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch
```
- Intrinsic Measurements 
```
https://developer.android.com/develop/ui/compose/layouts/intrinsic-measurements
```
- Dragging, Touch Inputs
```
https://developer.android.com/develop/ui/compose/touch-input/pointer-input/drag-swipe-fling

```

## Core Components

### 1. DrawingCanvas (UI Layer)
**Location**: `ui/DrawingCanvas.kt`

The main composable that handles all drawing operations and user interactions.

```kotlin
@Composable
fun DrawingCanvas(
    state: DrawingState,           // Current drawing state
    snapEngine: SnapEngine,        // Magnetic snapping engine
    onAction: (DrawingAction) -> Unit, // Action dispatcher
    modifier: Modifier = Modifier
)
```

**Key Responsibilities:**
- Handle touch gestures (tap, drag)
- Render all drawing elements
- Manage tool-specific interactions
- Coordinate with SnapEngine for magnetic snapping

### 2. DrawingState (Model Layer)
**Location**: `data/DrawingState.kt`

Centralized state that holds all drawing information.

```kotlin
data class DrawingState(
    val currentTool: DrawingTool = DrawingTool.Freehand,
    val elements: List<DrawingElement> = emptyList(),
    val isDrawing: Boolean = false,
    val canvasOffset: Offset = Offset.Zero,
    val canvasScale: Float = 1f,
    val strokeColor: Color = Color.Black,
    val strokeWidth: Float = 2f,
    val snapEnabled: Boolean = true,
    val rulerTool: RulerTool = RulerTool(),
    val compassTool: CompassTool = CompassTool(),
    val protractorTool: ProtractorTool = ProtractorTool(),
    val setSquareTool: SetSquareTool = SetSquareTool(),
    val gridSpacing: Float = 20f
)
```

### 3. DrawingViewModel (ViewModel Layer)
**Location**: `viewmodel/DrawingViewModel.kt`

Manages state changes and business logic.

```kotlin
class DrawingViewModel : ViewModel() {
    private val _drawingState = mutableStateOf(DrawingState())
    val drawingState: State<DrawingState> = _drawingState
    
    fun handleAction(action: DrawingAction) {
        // Process actions and update state
    }
}
```

## How to Run the Project

- Clone the Repository:
```
https://github.com/gunishjain/SnappyRulerSet.git
cd SnappyRulerSet
```
- Make Sure to check AGP Version, to avoid compose compiler sync issues,this project uses AGP version 8.9.1. 
- Build and run the Project

## Complete Project Structure

```
            ├───data
            │   ├───api
            │   ├───model
            │   └───repository
            ├───di
            │   └───module
            ├───navigation
            ├───ui
            │   ├───base
            │   ├───newslist
            │   ├───search
            │   ├───selections
            │   ├───sources
            │   ├───theme
            │   └───topheadlines
            └───utils
                NewsApplication.kt

```

### Working Demo

![](https://github.com/gunish
