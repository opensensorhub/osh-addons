# Rules Based Engine Rule Editor

### Rule Editor

To use the rule editor ui, the following configuration must be added to the admin UI configuration:

```
  ...
  {
    "objClass": "org.sensorhub.ui.AdminUIConfig",
    "widgetSet": "org.sensorhub.ui.SensorHubWidgetSet",
    "bundleRepoUrls": [],
    "customPanels": [],
    "customForms": [
      {
        "objClass": "org.sensorhub.ui.CustomUIConfig",
        "configClass": "com.georobotix.ai.impl.rulesengine.config.RuleEditor",
        "uiClass": "com.georobotix.ui.rulesengine.editor.RuleEditorForm"
      }
    ],
  ...
```

### Extending the Rule Editor

The default rule editor is a simple class in the rules-engine module. It is currently used by the discovery module, but
can be used by other modules by extending the `RuleEditor` class.  The config would need to be added to the admin UI
configuration as described above using the derived class you defined.