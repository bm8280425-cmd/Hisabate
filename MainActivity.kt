package com.example.hisabati

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

data class Voucher(val id:Int,val type:String,val name:String,val amount:Double,val note:String,val date:String)
class MainVM: ViewModel(){
 var vouchers by mutableStateOf(listOf<Voucher>()); private set; private var nextId=1
 fun add(type:String,name:String,amount:Double,note:String){ vouchers=listOf(Voucher(nextId++,type,name,amount,note,SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(Date())))+vouchers }
 val income get()=vouchers.filter{it.type=="قبض"}.sumOf{it.amount}; val expense get()=vouchers.filter{it.type=="صرف"}.sumOf{it.amount}; val balance get()=income-expense
}
@Composable fun App(vm:MainVM=viewModel()){
 var screen by remember{mutableStateOf("home")}; var type by remember{mutableStateOf("قبض")}
 MaterialTheme{Scaffold(topBar={TopAppBar(title={Text("حساباتي",fontWeight=FontWeight.Bold)})},bottomBar={NavigationBar{
  listOf("home" to "الرئيسية","vouchers" to "السندات","reports" to "التقارير").forEach{(id,label)->NavigationBarItem(selected=screen==id,onClick={screen=id},icon={},label={Text(label)})}
 }}){p->Box(Modifier.padding(p).fillMaxSize()){when(screen){"home"->Home(vm,{type="قبض";screen="add"},{type="صرف";screen="add"});"vouchers"->Vouchers(vm);"reports"->Reports(vm);"add"->AddVoucher(vm,type){screen="home"}}}}}
}
@Composable fun Home(vm:MainVM,inClick:()->Unit,outClick:()->Unit){LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("لوحة التحكم",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)};item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text("رصيد الصندوق");Text("${"%,.2f".format(vm.balance)} ريال",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)}}};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){Button(inClick,Modifier.weight(1f)){Text("＋ سند قبض")};Button(outClick,Modifier.weight(1f)){Text("− سند صرف")}}};item{Summary("إجمالي المقبوضات","${"%,.2f".format(vm.income)} ريال")};item{Summary("إجمالي المصروفات","${"%,.2f".format(vm.expense)} ريال")};item{Text("آخر السندات",fontWeight=FontWeight.Bold)};items(vm.vouchers.take(5)){VoucherRow(it)}}}
@Composable fun Summary(a:String,b:String)=Card(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(a);Text(b,fontWeight=FontWeight.Bold)}}
@Composable fun AddVoucher(vm:MainVM,type:String,done:()->Unit){var name by remember{mutableStateOf("")};var amount by remember{mutableStateOf("")};var note by remember{mutableStateOf("")};Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("سند $type",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);OutlinedTextField(name,{name=it},label={Text(if(type=="قبض")"قبض من" else "صرف إلى")},modifier=Modifier.fillMaxWidth());OutlinedTextField(amount,{amount=it.filter{c->c.isDigit()||c=='.'}},label={Text("المبلغ")},modifier=Modifier.fillMaxWidth());OutlinedTextField(note,{note=it},label={Text("البيان")},modifier=Modifier.fillMaxWidth(),minLines=3);Button(onClick={amount.toDoubleOrNull()?.let{vm.add(type,name,it,note);done()}},enabled=name.isNotBlank()&&amount.toDoubleOrNull()!=null,modifier=Modifier.fillMaxWidth()){Text("حفظ السند")}}}
@Composable fun Vouchers(vm:MainVM){Column(Modifier.padding(16.dp)){Text("سجل السندات",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);LazyColumn{items(vm.vouchers){VoucherRow(it)}}}}
@Composable fun VoucherRow(v:Voucher)=Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(14.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${v.type} #${v.id}",fontWeight=FontWeight.Bold);Text("${"%,.2f".format(v.amount)} ريال")};Text(v.name);Text(v.note);Text(v.date,style=MaterialTheme.typography.bodySmall)}}
@Composable fun Reports(vm:MainVM){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("التقارير",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Summary("إجمالي القبض","${"%,.2f".format(vm.income)} ريال");Summary("إجمالي الصرف","${"%,.2f".format(vm.expense)} ريال");Summary("صافي الحركة","${"%,.2f".format(vm.balance)} ريال");Text("عدد السندات: ${vm.vouchers.size}")}}
class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}
