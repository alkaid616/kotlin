// DUMP_IR
// PROVIDE_PREBUILT_DEPENDENCIES:com/example/myModule/OtherModule,com/example/myModule/AnotherModule

// MODULE: ui
// MODULE_KIND: LibraryBinary
// FILE: com/example/ui/Text.kt
package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Text(text: String, modifier: Modifier) {}

// MODULE: myModule
// FILE: com/example/myModule/OtherModule.kt
package com.example.myModule

class OtherModule {
    inline fun giveMeString() : String {
        return secret()
    }

    @PublishedApi
    internal fun secret() : String {
        return "what is up!!!!!!!"
    }
}

// FILE: com/example/myModule/AnotherModule.kt
package com.example.myModule

class AnotherModule {
    inline fun giveMeAnotherString() : String {
        return secret()
    }

    @PublishedApi
    internal fun secret() : String {
        return "what is up!!!!!!!"
    }
}

// MODULE: main(myModule, ui)
// FILE: main.kt
package home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.myModule.OtherModule
import com.example.myModule.AnotherModule
import com.example.ui.Text

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "$name!" + OtherModule().giveMeString() + AnotherModule().giveMeAnotherString(),
        modifier = modifier
    )
}
