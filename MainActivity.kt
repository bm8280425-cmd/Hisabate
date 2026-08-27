package com.example.hisabati

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

 data class Voucher(val id:Long,val number:Int,val type:String,val party:String,val amount:Double,val method:String,val note:String,val date:String)

class Db(ctx:Context):SQLiteOpenHelper(ctx,"hisabati.db",null,1){
 override fun onCreate(d:SQLiteDatabase){
  d.execSQL("CREATE TABLE vouchers(id INTEGER PRIMARY KEY AUTOINCREMENT, number INTEGER UNIQUE NOT NULL, type TEXT NOT NULL, party TEXT NOT NULL, amount REAL NOT NULL, method TEXT NOT NULL, note TEXT, date TEXT NOT NULL)")
  d.execSQL("CREATE TABLE settings(k TEXT PRIMARY KEY, v TEXT NOT NULL)")
  d.execSQL("INSERT INTO settings(k,v) VALUES('company','حساباتي'),('currency','ريال'),('opening','0'),('pin','')")
 }
 override fun onUpgrade(d:SQLiteDatabase,o:Int,n:Int){}
 fun nextNumber(type:String):Int { val c=readableDatabase.rawQuery("SELECT COALESCE(MAX(number),0)+1 FROM vouchers WHERE type=?",arrayOf(type)); c.moveToFirst(); val x=c.getInt(0); c.close(); return x }
 fun all():List<Voucher>{ val r=mutableListOf<Voucher>(); val c=readableDatabase.rawQuery("SELECT id,number,type,party,amount,method,note,date FROM vouchers ORDER BY id DESC",null); while(c.moveToNext()) r+=Voucher(c.getLong(0),c.getInt(1),c.getString(2),c.getString(3),c.getDouble(4),c.getString(5),c.getString(6),c.getString(7)); c.close(); return r }
 fun insert(v:Voucher){ val x=ContentValues().apply{put("number",v.number);put("type",v.type);put("party",v.party);put("amount",v.amount);put("method",v.method);put("note",v.note);put("date",v.date)}; writableDatabase.insertOrThrow("vouchers",null,x) }
 fun delete(id:Long){writableDatabase.delete("vouchers","id=?",arrayOf(id.toString()))}
 fun setting(k:String):String{val c=readableDatabase.rawQuery("SELECT v FROM settings WHERE k=?",arrayOf(k)); val s=if(c.moveToFirst())c.getString(0) else "";c.close();return s}
 fun set(k:String,v:String){val x=ContentValues().apply{put("k",k);put("v",v)};writableDatabase.insertWithOnConflict("settings",null,x,SQLiteDatabase.CONFLICT_REPLACE)}
 fun backup(uri:Uri,ctx:Context){ctx.contentResolver.openOutputStream(uri)?.bufferedWriter().use{w->w?.append("id,number,type,party,amount,method,note,date\n"); for(v in all())w?.append("${v.id},${v.number},${v.type},${csv(v.party)},${v.amount},${v.method},${csv(v.note)},${v.date}\n")}}
 private fun csv(s:String)=if(s.contains(",")||s.contains("\n"))"\"${s.replace("\"","\"\"")}\"" else s
}

class VM(private val ctx:Context):ViewModel(){
 private val db=Db(ctx); var list by mutableStateOf(db.all()); private set
 var company by mutableStateOf(db.setting("company")); var currency by mutableStateOf(db.setting("currency")); var opening by mutableStateOf(db.setting("opening").toDoubleOrNull()?:0.0)
 val income get()=list.filter{it.type=="قبض"}.sumOf{it.amount}; val expense get()=list.filter{it.type=="صرف"}.sumOf{it.amount}; val balance get()=opening+income-expense
 fun add(type:String,party:String,amount:Double,method:String,note:String){db.insert(Voucher(0,db.nextNumber(type),type,party,amount,method,note,now()));list=db.all()}
 fun delete(id:Long){db.delete(id);list=db.all()}
 fun saveSettings(c:String,cur:String,op:String){db.set("company",c);db.set("currency",cur);db.set("opening",op);company=c;currency=cur;opening=op.toDoubleOrNull()?:0.0}
 fun backup(uri:Uri)=db.backup(uri,ctx)
 private fun now()=SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(Date())
}

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}

@Composable fun App(vm:VM=viewModel(factory=object:androidx.lifecycle.ViewModelProvider.Factory{override fun <T:ViewModel> create(c:Class<T>)=VM(castContext()) as T})){var tab by remember{mutableStateOf(0)}; MaterialTheme{Scaffold(topBar={TopAppBar(title={Text(vm.company)})},bottomBar={NavigationBar{listOf("الرئيسية","السندات","التقارير","الإعدادات").forEachIndexed{i,s->NavigationBarItem(i==tab,{tab=i},icon={},label={Text(s)})}}}){p->Box(Modifier.padding(p)){when(tab){0->Home(vm);1->Vouchers(vm);2->Reports(vm);3->Settings(vm)}}}}}

@Composable fun Home(vm:VM){var type by remember{mutableStateOf<String?>(null)}; Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("لوحة التحكم",style=MaterialTheme.typography.headlineSmall);Card{Column(Modifier.padding(18.dp)){Text("رصيد الصندوق");Text("${fmt(vm.balance)} ${vm.currency}",style=MaterialTheme.typography.headlineMedium)}};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({type="قبض"},Modifier.weight(1f)){Text("＋ قبض")};Button({type="صرف"},Modifier.weight(1f)){Text("− صرف")}};Summary("إجمالي المقبوضات","${fmt(vm.income)} ${vm.currency}");Summary("إجمالي المصروفات","${fmt(vm.expense)} ${vm.currency}");if(type!=null)VoucherForm(vm,type!!){type=null}}}
@Composable fun VoucherForm(vm:VM,type:String,done:()->Unit){var party by remember{mutableStateOf("")};var amount by remember{mutableStateOf("")};var method by remember{mutableStateOf("نقدي")};var note by remember{mutableStateOf("")};Card{Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("سند $type");OutlinedTextField(party,{party=it},label={Text(if(type=="قبض")"قبض من" else "صرف إلى")},Modifier.fillMaxWidth());OutlinedTextField(amount,{amount=it.filter{c->c.isDigit()||c=='.'}},label={Text("المبلغ")},Modifier.fillMaxWidth());OutlinedTextField(method,{method=it},label={Text("طريقة الدفع")},Modifier.fillMaxWidth());OutlinedTextField(note,{note=it},label={Text("البيان")},Modifier.fillMaxWidth());Button({amount.toDoubleOrNull()?.let{vm.add(type,party,it,method,note);done()}},enabled=party.isNotBlank()&&amount.toDoubleOrNull()!=null,Modifier.fillMaxWidth()){Text("حفظ")}}}}
@Composable fun Vouchers(vm:VM){LazyColumn(Modifier.fillMaxSize().padding(16.dp)){item{Text("سجل السندات",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(8.dp))};items(vm.list){v->VoucherRow(v){vm.delete(v.id)}}}}
@Composable fun VoucherRow(v:Voucher,onDelete:()->Unit)=Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${v.type} #${v.number}");Text(fmt(v.amount))};Text(v.party);Text("${v.method} • ${v.date}",style=MaterialTheme.typography.bodySmall);if(v.note.isNotBlank())Text(v.note);TextButton(onDelete){Text("حذف")}}}
@Composable fun Reports(vm:VM){Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("التقارير",style=MaterialTheme.typography.headlineSmall);Summary("الرصيد الافتتاحي","${fmt(vm.opening)} ${vm.currency}");Summary("القبض","${fmt(vm.income)} ${vm.currency}");Summary("الصرف","${fmt(vm.expense)} ${vm.currency}");Summary("الرصيد الحالي","${fmt(vm.balance)} ${vm.currency}");Text("عدد السندات: ${vm.list.size}")}}
@Composable fun Settings(vm:VM){var c by remember{mutableStateOf(vm.company)};var cur by remember{mutableStateOf(vm.currency)};var op by remember{mutableStateOf(vm.opening.toString())};val launcher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){it?.let{vm.backup(it)}};Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("الإعدادات",style=MaterialTheme.typography.headlineSmall);OutlinedTextField(c,{c=it},label={Text("اسم المنشأة")},Modifier.fillMaxWidth());OutlinedTextField(cur,{cur=it},label={Text("العملة")},Modifier.fillMaxWidth());OutlinedTextField(op,{op=it},label={Text("الرصيد الافتتاحي")},Modifier.fillMaxWidth());Button({vm.saveSettings(c,cur,op)},Modifier.fillMaxWidth()){Text("حفظ الإعدادات")};Button({launcher.launch("hisabati_backup.csv")},Modifier.fillMaxWidth()){Text("نسخة احتياطية CSV")}}}
@Composable fun Summary(a:String,b:String)=Card(Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(a);Text(b)}}
fun fmt(x:Double)="%,.2f".format(Locale.US,x)
@Composable fun castContext():Context = androidx.compose.ui.platform.LocalContext.current
