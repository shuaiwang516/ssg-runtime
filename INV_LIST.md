Supported likely invariants
```
Special Value over primitive types (Short, Int, Long, Float, Double)
val is null
val = -1
val = 0
val = 1
val = rest (except the above values)


Special Value sequences (Array/Collection)
val is null
val.size = 0
val.size = 1
val.size = rest


String: String is null, length = 0 (empty), length = 1, length = rest
val is null
val.length = 0
val.length = 1
val.length = rest


Boolean: True, False
val is null
val is True
val is False


Object Type
val is null
val.class belongs to {Class1, Class2, …}
If it’s enum type: serialized constant belongs to {CONSTANT1, CONSTANT2, …}


Equality likely invariants, including multiple vals. It applies to types whose instances could be compared in the new version. (key type in map or set type).
val1 == val2 == …


IsSerialized
Suppose ClassA.field is modified across versions, whether ClassA.field is serialized.
Suppose EnumA is modified across versions. For each constant in EnumA, whether the constant is serialized.


Multiple likely invariant broken at the same time
Broken at the same time: {inv1, inv2, …}


===


Boundary-related invariant (not in use)
boundary comparison is true or false.
boundary gap likely invariant: |a - b| < val

```