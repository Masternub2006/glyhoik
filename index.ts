//import { registerRootComponent } from "expo";
import { AppRegistry } from "react-native";
import App from "./App";
import {name as appName} from "./app.json";

console.log("[AppRegistry] Registering App component");

//registerRootComponent(App);
AppRegistry.registerComponent(appName, () => App);