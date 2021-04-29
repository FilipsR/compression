# LZ77

Izvads satur ascii simbolus un pārus {*garums*, *distance*}.

Vienkāršības labad, katru elementu aprakstam ar vismaz 2 baitiem:
* 1 baits *garums*
* 2 baiti *distance*, ja *garums* nav nulle
* 1 baits - ascii simbols, ja *garums* ir nulle

# Arithmetic Coding

Izmanto divus alfabētus:
1. ascii simboli, garumi un "End-of-File"
2. distances

Distance var atrasties tikai pēc garuma.
