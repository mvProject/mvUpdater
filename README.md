# mvUpdater
[![](https://jitpack.io/v/mvProject/mvUpdater.svg)](https://jitpack.io/#mvProject/mvUpdater)

Simple Implementation of app autoupdating feature

#### Install

##### root build.gradle
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

##### app build.gradle

	dependencies {
	        implementation 'com.github.mvProject:mvUpdater:version'
	}
        
#### Json example

        {
          "latestVersion": "1.0.1",
          "file": "app-debug.apk",
          "url": "https://github.com/mvProject/mvUpdater/blob/master/app/release/app-debug.apk?raw=true"
        }

#### Usage

##### Manifest
add provider to ApplicationManifest       
      
      <provider
          android:name="androidx.core.content.FileProvider"
          android:authorities="${applicationId}.provider"
          android:exported="false"
          android:grantUriPermissions="true">
          <meta-data
              android:name="android.support.FILE_PROVIDER_PATHS"
              android:resource="@xml/paths"/>
          </provider>
   
##### Paths
create file paths.xml at resource directory

          <paths>
             <external-path name="external_files" path="."/>
          </paths>

##### MainActivity 

          private val json = "https://raw.githubusercontent.com/mvProject/mvUpdater/master/app/release/update.json"
          override fun onCreate(savedInstanceState: Bundle?) {
          ...
          ...
          val upd = Updater(this)
          ...
          ...
          }             
