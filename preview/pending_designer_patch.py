from pathlib import Path

path = Path('preview/shape-editor.html')
s = path.read_text(encoding='utf-8')


def rep(old: str, new: str) -> None:
    global s
    if old not in s:
        raise SystemExit('Expected designer pattern not found:\n' + old[:300])
    s = s.replace(old, new, 1)

# Vector boolean engine (used only inside the browser designer).
rep('</head>', '<script src="https://unpkg.com/paper@0.12.18/dist/paper-full.min.js"></script>\n</head>')

rep(
'''      <div class="field"><label>Opacity</label><input id="opacity" type="number" min="0" max="1" step="0.05" value="0.5"></div>\n      <div class="checks"><label><input id="visible" type="checkbox" checked> Visible</label><label><input id="locked" type="checkbox"> Locked</label><label><input id="smooth" type="checkbox" checked> Smooth path</label></div>\n    </div>''',
'''      <div class="field"><label>Opacity</label><input id="opacity" type="number" min="0" max="1" step="0.05" value="0.5"></div>\n      <div class="row" style="margin:8px 0"><button class="btn" id="eraseFill">Erase Fill</button><button class="btn" id="eraseStroke">Erase Border</button></div>\n      <div class="row" style="margin:8px 0"><button class="btn" id="restoreFill">Restore Fill</button><button class="btn" id="restoreStroke">Restore Border</button></div>\n      <div class="checks"><label><input id="visible" type="checkbox" checked> Visible</label><label><input id="locked" type="checkbox"> Locked</label><label><input id="smooth" type="checkbox" checked> Smooth path</label></div>\n    </div>\n\n    <div class="section"><h3>Combine shapes</h3>\n      <div class="field"><label>Second shape</label><select id="combineWith"></select></div>\n      <div class="row"><button class="btn primary" id="mergeShape">Merge / Union</button><button class="btn" id="subtractShape">Subtract</button></div>\n      <div class="row" style="margin-top:7px"><button class="btn" id="intersectShape">Intersect</button></div>\n      <div class="muted" style="margin-top:7px">Merge creates one combined outline. Subtract cuts the selected second shape out of the current shape. The result becomes a normal editable layer with points.</div>\n    </div>'''
)

rep(
'''function makeLayer(name,points,opt={}){return{id:uid(),name,type:opt.type||'shape',points:points.map(p=>[p[0],p[1]]),closed:opt.closed!==false,fill:opt.fill||'#b9dffa',stroke:opt.stroke||'#ffffff',strokeWidth:opt.strokeWidth??1.5,opacity:opt.opacity??.45,visible:opt.visible!==false,locked:!!opt.locked,smooth:opt.smooth!==false,bind:opt.bind||null}}''',
'''function makeLayer(name,points,opt={}){return{id:uid(),name,type:opt.type||'shape',points:points.map(p=>[p[0],p[1]]),closed:opt.closed!==false,fill:opt.fill||'#b9dffa',fillOn:opt.fillOn!==false,stroke:opt.stroke||'#ffffff',strokeOn:opt.strokeOn!==false,strokeWidth:opt.strokeWidth??1.5,opacity:opt.opacity??.45,visible:opt.visible!==false,locked:!!opt.locked,smooth:opt.smooth!==false,bind:opt.bind||null}}'''
)

rep(
'''const svg=document.getElementById('canvas'),statusBox=document.getElementById('status');''',
'''const svg=document.getElementById('canvas'),statusBox=document.getElementById('status');\nconst paperCanvas=document.createElement('canvas');paperCanvas.width=W;paperCanvas.height=H;paperCanvas.style.display='none';document.body.appendChild(paperCanvas);paper.setup(paperCanvas);'''
)

rep(
'''const p=svgEl('path',{d:shapePath(l),fill:l.closed?l.fill:'none',stroke:l.stroke,'stroke-width':l.strokeWidth,opacity:l.opacity,'data-layer':l.id,class:'shapePath'});''',
'''const p=svgEl('path',{d:shapePath(l),fill:l.closed&&l.fillOn!==false?l.fill:'none',stroke:l.strokeOn!==false?l.stroke:'none','stroke-width':l.strokeWidth,opacity:l.opacity,'data-layer':l.id,class:'shapePath'});'''
)

rep(
'''function renderInspector(){const l=selected(),disabled=!l;['name','fill','stroke','strokeWidth','opacity','visible','locked','smooth'].forEach(id=>document.getElementById(id).disabled=disabled);if(!l){document.getElementById('pointList').innerHTML='';return}document.getElementById('name').value=l.name;document.getElementById('fill').value=l.fill;document.getElementById('stroke').value=l.stroke;document.getElementById('strokeWidth').value=l.strokeWidth;document.getElementById('opacity').value=l.opacity;document.getElementById('visible').checked=l.visible;document.getElementById('locked').checked=l.locked;document.getElementById('smooth').checked=l.smooth;const pbox=document.getElementById('pointList');''',
'''function renderInspector(){const l=selected(),disabled=!l;['name','fill','stroke','strokeWidth','opacity','visible','locked','smooth','eraseFill','eraseStroke','restoreFill','restoreStroke','combineWith','mergeShape','subtractShape','intersectShape'].forEach(id=>document.getElementById(id).disabled=disabled);if(!l){document.getElementById('pointList').innerHTML='';document.getElementById('combineWith').innerHTML='';return}if(l.fillOn==null)l.fillOn=true;if(l.strokeOn==null)l.strokeOn=true;document.getElementById('name').value=l.name;document.getElementById('fill').value=l.fill;document.getElementById('stroke').value=l.stroke;document.getElementById('strokeWidth').value=l.strokeWidth;document.getElementById('opacity').value=l.opacity;document.getElementById('visible').checked=l.visible;document.getElementById('locked').checked=l.locked;document.getElementById('smooth').checked=l.smooth;const combo=document.getElementById('combineWith');const old=combo.value;combo.innerHTML=layers.filter(x=>x.id!==l.id&&x.visible&&x.points.length>=3).map(x=>`<option value="${x.id}">${escapeHtml(x.name)}</option>`).join('');if([...combo.options].some(o=>o.value===old))combo.value=old;const pbox=document.getElementById('pointList');'''
)

anchor = "applyProp('name',(l,e)=>l.name=e.value||'Shape');applyProp('fill',(l,e)=>l.fill=e.value);applyProp('stroke',(l,e)=>l.stroke=e.value);"
insert = """applyProp('name',(l,e)=>l.name=e.value||'Shape');applyProp('fill',(l,e)=>{l.fill=e.value;l.fillOn=true});applyProp('stroke',(l,e)=>{l.stroke=e.value;l.strokeOn=true});"""
rep(anchor, insert)

rep(
'''document.querySelectorAll('.tool[data-tool]').forEach(b=>b.onclick=()=>setTool(b.dataset.tool));document.getElementById('backMain').onclick=()=>location.href='./';''',
'''function paperPathFromLayer(l){const p=new paper.Path();l.points.forEach(q=>p.add(new paper.Point(q[0],q[1])));p.closed=true;if(l.smooth&&l.points.length>3)p.smooth({type:'catmull-rom',factor:.5});return p}\nfunction pointsFromBoolean(item){let p=item;if(item.className==='CompoundPath'){const candidates=item.children.filter(c=>c.closed);if(!candidates.length)return null;p=candidates.sort((a,b)=>Math.abs(b.area)-Math.abs(a.area))[0]}if(!p.closed)return null;p.flatten(4);const pts=p.segments.map(seg=>[clamp(seg.point.x,0,W),clamp(seg.point.y,0,H)]);if(pts.length>80){const step=Math.ceil(pts.length/80);return pts.filter((_,i)=>i%step===0)}return pts.length>=3?pts:null}\nfunction combineSelected(kind){const a=selected(),id=document.getElementById('combineWith').value,b=findLayer(id);if(!a||!b){statusBox.innerHTML='<span class="bad">Choose a second shape first.</span>';return}checkpoint();const pa=paperPathFromLayer(a),pb=paperPathFromLayer(b);let result;if(kind==='union')result=pa.unite(pb);else if(kind==='subtract')result=pa.subtract(pb);else result=pa.intersect(pb);const pts=pointsFromBoolean(result);pa.remove();pb.remove();result.remove();if(!pts){statusBox.innerHTML='<span class="bad">This result cannot be converted to one editable outline. Try overlapping the shapes differently.</span>';return}const bind=a.bind||b.bind||null;layers=layers.filter(x=>x.id!==a.id&&x.id!==b.id);const merged=makeLayer(kind==='union'?'Merged Shape':kind==='subtract'?'Subtracted Shape':'Intersection',pts,{fill:a.fill,fillOn:a.fillOn!==false,stroke:a.stroke,strokeOn:a.strokeOn!==false,strokeWidth:a.strokeWidth,opacity:a.opacity,smooth:false,bind});layers.push(merged);selectedId=merged.id;selectedNode=null;syncBound();pushHistory();render();statusBox.innerHTML='<span class="ok">Vector operation complete. The result is fully editable.</span>'}\ndocument.getElementById('eraseFill').onclick=()=>{const l=selected();if(!l)return;checkpoint();l.fillOn=false;pushHistory();render()};\ndocument.getElementById('restoreFill').onclick=()=>{const l=selected();if(!l)return;checkpoint();l.fillOn=true;pushHistory();render()};\ndocument.getElementById('eraseStroke').onclick=()=>{const l=selected();if(!l)return;checkpoint();l.strokeOn=false;pushHistory();render()};\ndocument.getElementById('restoreStroke').onclick=()=>{const l=selected();if(!l)return;checkpoint();l.strokeOn=true;pushHistory();render()};\ndocument.getElementById('mergeShape').onclick=()=>combineSelected('union');document.getElementById('subtractShape').onclick=()=>combineSelected('subtract');document.getElementById('intersectShape').onclick=()=>combineSelected('intersect');\ndocument.querySelectorAll('.tool[data-tool]').forEach(b=>b.onclick=()=>setTool(b.dataset.tool));document.getElementById('backMain').onclick=()=>location.href='./';'''
)

path.write_text(s, encoding='utf-8')
print('Designer erase + boolean vector tools applied.')
