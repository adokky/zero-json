Testing framework designed specifically for `zero-json` library.

* Simple assertion utilities like `assertStringFormAndRestored`.
* Lots of `kotlinx.json.JsonElement` utilities (mainly randomization).
* `RandomizedJsonTest` - main base class for automatic parametrized tests:
  * Declarative test specification DSL.
  * Automatically tests all encoder/decoder combinations.
  * Key order randomization.
  * Random key insertion.
  * Random space insertion.
  * and many more