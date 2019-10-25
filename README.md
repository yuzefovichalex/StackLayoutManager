# StackLayoutManager
Custom LayoutManager for providing stack behavior for RecyclerView list items.
## Description
This layout manager is used to display full-sized (**MATCH_PARENT**) elements in the form of a stack.
If you set not MATCH_PARENT size for item, StackLayoutManager automatically re-measure it.
Project contains custom SnapHelper for providing snapping effect with StackLayoutManager.
Currently supported only vertical orientation of RecyclerView!
## Samples
### Basic implementation:
![](/samples/basic.gif)
### With snapping (using SmartPagerSnapHepler):
![](/samples/with_snapping.gif)
## Usage
### Adding dependency
In root build.gradle:
```gradle
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
In module build.gradle:
```gradle
dependencies {
	        implementation 'com.github.yuzefovichalex:StackLayoutManager:1.1.0'
	}
```
### Basic usage of StackLayoutManager
```Java
StackLayoutManager stackLayoutManager = new StackLayoutManager();
recyclerView.setLayoutManager(stackLayoutManager);
```
For changing **scaleFactor**:
```Java
stackLayoutManager.setScaleFactor(0.5f);
```
For adding snapping effect:
```Java
SmartPagerSnapHelper smartPagerSnapHelper = new SmartPagerSnapHelper();
smartPagerSnapHelper.attachToRecyclerView(recyclerView);
```
For adding bottom offset effect (top of the bottom item is visible) in XML with your RecyclerView:
```XML
<androidx.recyclerview.widget.RecyclerView
        ...
        android:paddingBottom="24dp"
        android:clipToPadding="false"/>
```
## License
    Copyright 2019 Alexander Yuzefovich.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
