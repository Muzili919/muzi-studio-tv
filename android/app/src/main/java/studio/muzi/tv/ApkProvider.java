package studio.muzi.tv;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;
/** Read-only APK access for the system installer, including Android 6. */
public class ApkProvider extends ContentProvider {
 @Override public boolean onCreate(){return true;}
 private File file(Uri u)throws FileNotFoundException{
  if(u==null||!"content".equals(u.getScheme())||!(getContext().getPackageName()+".apk").equals(u.getAuthority())||u.getPathSegments().size()!=1)throw new FileNotFoundException("Invalid APK URI");
  String name=u.getLastPathSegment();if(name==null||!name.endsWith(".apk")||name.contains("/")||name.contains("\\")||name.startsWith("."))throw new FileNotFoundException("Invalid APK filename");
  File f=new File(new File(getContext().getCacheDir(),"update"),name);if(!f.isFile())throw new FileNotFoundException("APK not found");return f;
 }
 @Override public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(file(u),ParcelFileDescriptor.MODE_READ_ONLY);}
 @Override public String getType(Uri u){return "application/vnd.android.package-archive";}
 @Override public Cursor query(Uri u,String[] columns,String selection,String[] args,String order){
  try{File f=file(u);String[] cols=columns==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:columns;MatrixCursor c=new MatrixCursor(cols,1);Object[] row=new Object[cols.length];for(int i=0;i<cols.length;i++){if(OpenableColumns.DISPLAY_NAME.equals(cols[i]))row[i]=f.getName();else if(OpenableColumns.SIZE.equals(cols[i]))row[i]=f.length();}c.addRow(row);return c;}catch(FileNotFoundException e){return null;}
 }
 @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException("Read only");}
 @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException("Read only");}
 @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException("Read only");}
}
