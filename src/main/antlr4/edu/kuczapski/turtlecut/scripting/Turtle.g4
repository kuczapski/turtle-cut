grammar Turtle;


program: commandBlock*;


command: cut | draw | repeate;

commandBlock: ('[' command* ']') | command;

repeate: REPEATKEY INT commandBlock;
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
			   ('spre' degrees ('cu lungime de')|('de') length );

coordinate: NUM ',' NUM;
degrees: 'stanga' | 'dreapta' | 'inainte' | (INT 'grade') ;
length: NUM ('mm'|'cm'|);

INT: [0-9]+;
NUM: [0-9]+('.'[0-9]);
WS: [ \t\r\n]+;

UNKNOWN: .; 