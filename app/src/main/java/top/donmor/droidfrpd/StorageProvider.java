package top.donmor.droidfrpd;

import static top.donmor.droidfrpd.Utils.E_FNF;
import static top.donmor.droidfrpd.Utils.KEY_EULA;
import static top.donmor.droidfrpd.Utils.S0;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class StorageProvider extends DocumentsProvider {
	private static final String RID = "files";
	private static final String[] PROJECTIONS_ROOT = new String[]{
			DocumentsContract.Root.COLUMN_ROOT_ID,
			DocumentsContract.Root.COLUMN_FLAGS,
			DocumentsContract.Root.COLUMN_TITLE,
			DocumentsContract.Root.COLUMN_DOCUMENT_ID,
			DocumentsContract.Root.COLUMN_ICON,
	};
	private static final String[] PROJECTIONS_DOC = new String[]{
			DocumentsContract.Document.COLUMN_DOCUMENT_ID,
			DocumentsContract.Document.COLUMN_DISPLAY_NAME,
			DocumentsContract.Document.COLUMN_SIZE,
			DocumentsContract.Document.COLUMN_MIME_TYPE,
			DocumentsContract.Document.COLUMN_LAST_MODIFIED,
			DocumentsContract.Document.COLUMN_FLAGS,
	};
	private static final String SLASH = "/";
	private static final String MIME_BIN = "application/octet-stream";
	private static final Uri NOTIFIER_URI = Uri.parse("content://top.donmor.droidfrpd.documents/files/.notify");

	private Context context;
	private File filesDir = null;

	@Override
	public boolean onCreate() {
		if ((context = getContext()) == null) return false;
		filesDir = context.getFilesDir();
		return true;
	}

	@Override
	public Cursor queryRoots(String[] projection) {
		final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
		if (context == null || !Utils.getPreferences(context).getBoolean(KEY_EULA, false)) return result;
		final MatrixCursor.RowBuilder row = result.newRow();
		row.add(DocumentsContract.Root.COLUMN_ROOT_ID, RID);
		row.add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_SUPPORTS_CREATE |
				DocumentsContract.Root.FLAG_LOCAL_ONLY);
		row.add(DocumentsContract.Root.COLUMN_TITLE, context.getString(R.string.app_name));
		row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, getDocIdForFile(filesDir));
		row.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_menu_settings);
		return result;
	}

	@Override
	public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
		final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
		includeFile(result, documentId, null);
		return result;
	}

	@Override
	public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
		final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
		final File[] parent = getFileForDocId(parentDocumentId).listFiles();
		if (parent == null) return result;
		for (File file : parent) {
			includeFile(result, null, file);
		}
		result.setNotificationUri(context.getContentResolver(), NOTIFIER_URI);
		return result;
	}

	@Override
	public ParcelFileDescriptor openDocument(String documentId, String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
		final File file = getFileForDocId(documentId);
		final int accessMode = ParcelFileDescriptor.parseMode(mode);
		return ParcelFileDescriptor.open(file, accessMode);
	}

	/**
	 * @noinspection ResultOfMethodCallIgnored
	 */
	@Override
	public String createDocument(String documentId, String mimeType, String displayName)
			throws FileNotFoundException {
		File file = new File(getFileForDocId(documentId), displayName);
		try {
			if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) file.mkdir();
			else file.createNewFile();
			file.setWritable(true);
			file.setReadable(true);
		} catch (IOException e) {
			throw new FileNotFoundException(e.getMessage());
		}
		context.getContentResolver().notifyChange(NOTIFIER_URI, null);
		return getDocIdForFile(file);
	}

	@Override
	public void deleteDocument(String documentId) throws FileNotFoundException {
		File file = getFileForDocId(documentId);
		purgePath(file);
		if (context != null) context.getContentResolver().notifyChange(NOTIFIER_URI, null);
	}

	@Override
	public void removeDocument(String documentId, String parentDocumentId) throws FileNotFoundException {
		deleteDocument(documentId);
	}

	@Override
	public boolean isChildDocument(String parentDocumentId, String documentId) {
		return documentId.startsWith(parentDocumentId);
	}

	@Override
	public String copyDocument(String sourceDocumentId, String targetParentDocumentId) throws FileNotFoundException {
		File src = getFileForDocId(sourceDocumentId), dst = getFileForDocId(targetParentDocumentId);
		try {
			Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new FileNotFoundException(e.getMessage());
		}
		context.getContentResolver().notifyChange(NOTIFIER_URI, null);
		return targetParentDocumentId;
	}

	@Override
	public String moveDocument(String sourceDocumentId, String sourceParentDocumentId, String targetParentDocumentId) throws FileNotFoundException {
		File src = getFileForDocId(sourceDocumentId), dst = new File(getFileForDocId(targetParentDocumentId), src.getName());
		if (!src.renameTo(dst)) throw new FileNotFoundException(String.format(E_FNF, src));
		context.getContentResolver().notifyChange(NOTIFIER_URI, null);
		return targetParentDocumentId;
	}

	@Override
	public String renameDocument(String documentId, String displayName) throws FileNotFoundException {
		File src = getFileForDocId(documentId), dst = new File(src.getParentFile(), displayName);
		if (!src.renameTo(dst)) throw new FileNotFoundException(String.format(E_FNF, src));
		context.getContentResolver().notifyChange(NOTIFIER_URI, null);
		return getDocIdForFile(dst);
	}

	private static String[] resolveRootProjection(String[] projection) {
		return projection != null ? projection : PROJECTIONS_ROOT;
	}

	private static String[] resolveDocumentProjection(String[] projection) {
		return projection != null ? projection : PROJECTIONS_DOC;
	}


	private void includeFile(MatrixCursor result, String docId, File file)
			throws FileNotFoundException {
		if (docId == null) {
			docId = getDocIdForFile(file);
		} else {
			file = getFileForDocId(docId);
		}

		int flags = getFileFlags(file);

		final String displayName = file.getName();
		final String mimeType = getTypeForFile(file);

		final MatrixCursor.RowBuilder row = result.newRow();
		row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, docId);
		row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName);
		row.add(DocumentsContract.Document.COLUMN_SIZE, file.length());
		row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType);
		row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified());
		row.add(DocumentsContract.Document.COLUMN_FLAGS, flags);
	}

	private static int getFileFlags(File file) {
		int flags = 0;

		if (file.isDirectory()) {
			if (file.isDirectory() && file.canWrite()) {
				flags |= DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE;
			}
		} else if (file.canWrite()) {
			flags |= DocumentsContract.Document.FLAG_SUPPORTS_WRITE;
			flags |= DocumentsContract.Document.FLAG_SUPPORTS_RENAME;
			flags |= DocumentsContract.Document.FLAG_SUPPORTS_MOVE;
			flags |= DocumentsContract.Document.FLAG_SUPPORTS_COPY;
		}
		flags |= DocumentsContract.Document.FLAG_SUPPORTS_REMOVE;
		flags |= DocumentsContract.Document.FLAG_SUPPORTS_DELETE;
		return flags;
	}

	private static String getTypeForFile(File file) {
		if (file.isDirectory()) {
			return DocumentsContract.Document.MIME_TYPE_DIR;
		} else {
			return getTypeForName(file.getName());
		}
	}

	private static String getTypeForName(String name) {
		final int lastDot = name.lastIndexOf('.');
		if (lastDot >= 0) {
			final String extension = name.substring(lastDot + 1);
			final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			if (mime != null) {
				return mime;
			}
		}
		return MIME_BIN;
	}

	private File getFileForDocId(String docId) throws FileNotFoundException {
		File target = filesDir;
		if (docId.equals(RID)) {
			return target;
		}
		final int splitIndex = docId.indexOf(':', 1);
		if (splitIndex < 0) {
			throw new FileNotFoundException(String.format(E_FNF, docId));
		} else {
			final String path = docId.substring(splitIndex + 1);
			target = new File(target, path);
			if (!target.exists()) {
				throw new FileNotFoundException(String.format(E_FNF, target));
			}
			return target;
		}
	}

	private String getDocIdForFile(File file) {
		String path = file.getAbsolutePath();
		final String rootPath = filesDir.getPath();
		if (rootPath.equals(path)) {
			path = S0;
		} else if (rootPath.endsWith(SLASH)) {
			path = path.substring(rootPath.length());
		} else {
			path = path.substring(rootPath.length() + 1);
		}

		return RID + ':' + path;
	}

	private static void purgePath(File file) throws FileNotFoundException {
		File[] files;
		if (file.isDirectory() && (files = file.listFiles()) != null)
			for (File f : files) purgePath(f);
		if (!file.delete()) throw new FileNotFoundException(String.format(E_FNF, file));
	}
}
