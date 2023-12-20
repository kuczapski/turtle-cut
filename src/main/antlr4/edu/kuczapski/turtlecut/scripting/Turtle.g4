grammar Turtle;


program: commandBlock*;


command: cut | draw | moveto | repeate ;

commandBlock: ('[' command* ']') | command;

repeate: REPEATKEY NUM commandBlock;
REPEATKEY: 'repeta';

cut: CUTKEY geometryBlock;
CUTKEY: 'taie';

draw: DRAWKEY geometryBlock;
DRAWKEY: 'deseneaza';

geometryBlock: ('[' geometry* ']') | geometry;

geometry: line | circle | moveto;

moveto: MOVETOKEY coordinate;
MOVETOKEY: 'du-te la';

line: LINEKEY startCoordinate endCoordinate;
LINEKEY: 'linie';

circle: CIRCLEKEY (('cu centrul in' coordinate)|()) ('cu raza de' length);
CIRCLEKEY: 'cerc';

startCoordinate: ('de la' coordinate)| ();

endCoordinate: ('pana la' coordinate)| 
			   ('spre' degrees (('cu lungime de')|('de')) length );

coordinate: NUM ',' NUM;
degrees: 'stanga' | 'dreapta' | 'inainte' | (NUM 'grade') ;
length: NUM ('mm'|'cm'|);


NUM: INT ('.' INT)?;
INT: [0-9]+;


WS: [ \t\r\n]+;

UNKNOWN: .;
