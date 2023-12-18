grammar Turtle;


program: commandBlock*;


command: cut | draw | repeate;

commandBlock: ('[' command* ']') | command;

repeate: 'repeta' INT commandBlock;

cut: 'taie' geometryBlock;
draw: 'deseneaza' geometryBlock;

geometryBlock: ('[' geometry* ']') | geometry;

geometry: line | circle;

line: 'linie' startCoordinate endCoordinate;
circle: 'cerc' (('cu centrul in' coordinate)|()) ('cu raza de' length);

startCoordinate: ('de la' coordinate)| ();

endCoordinate: ('pana la' coordinate)| 
			   ('spre' degrees ('cu lungime de')|('de') length );

coordinate: INT ',' INT;
degrees: 'stanga' | 'dreapta' | 'inainte' | (INT 'grade') ;
length: INT 'mm'|'cm'|;

INT: [0-9]+ ;
WS: [ \t\r\n]+ -> skip ;