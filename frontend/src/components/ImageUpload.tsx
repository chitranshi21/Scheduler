import { useState, useRef } from 'react';

interface ImageUploadProps {
  currentImageUrl?: string;
  onUpload: (file: File) => Promise<string>;
  onError?: (error: string) => void;
}

const MAX_FILE_SIZE_MB = 5;
const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;
const ALLOWED_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

export default function ImageUpload({ currentImageUrl, onUpload, onError }: ImageUploadProps) {
  const [preview, setPreview] = useState<string | null>(currentImageUrl || null);
  const [uploading, setUploading] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  };

  const validateFile = (file: File): string | null => {
    if (!ALLOWED_TYPES.includes(file.type)) {
      return `Invalid file type. Please upload JPG, PNG, or WebP images only.\n\nYour file type: ${file.type}`;
    }

    if (file.size > MAX_FILE_SIZE_BYTES) {
      const fileSize = formatFileSize(file.size);
      return `File size exceeds the maximum limit.\n\nYour file: ${fileSize}\nMaximum allowed: ${MAX_FILE_SIZE_MB} MB\n\nPlease choose a smaller image.`;
    }

    return null;
  };

  const handleFileSelect = async (file: File) => {
    // Validate file
    const error = validateFile(file);
    if (error) {
      if (onError) onError(error);
      alert(error);
      return;
    }

    // Store selected file info
    setSelectedFile(file);

    // Show preview
    const reader = new FileReader();
    reader.onloadend = () => {
      setPreview(reader.result as string);
    };
    reader.readAsDataURL(file);

    // Upload file
    try {
      setUploading(true);
      const imageUrl = await onUpload(file);
      console.log('✅ Image uploaded successfully:', imageUrl);
      setSelectedFile(null); // Clear after successful upload
    } catch (err: any) {
      console.error('❌ Upload failed:', err);

      // Handle 413 (Payload Too Large) specifically
      let errorMessage = '';
      if (err.response?.status === 413) {
        errorMessage = `File too large for server.\n\nYour file: ${formatFileSize(file.size)}\nMaximum allowed: ${MAX_FILE_SIZE_MB} MB\n\nPlease choose a smaller image or compress your current image.`;
      } else {
        errorMessage = err.response?.data?.message || err.message || 'Failed to upload image';
      }

      if (onError) onError(errorMessage);
      alert('Upload failed:\n\n' + errorMessage);

      // Revert preview on error
      setPreview(currentImageUrl || null);
      setSelectedFile(null);
    } finally {
      setUploading(false);
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);

    const file = e.dataTransfer.files?.[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  return (
    <div style={{ marginBottom: '20px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <label className="form-label" style={{ margin: 0 }}>Logo Image</label>
        <span style={{
          fontSize: '12px',
          color: '#6b7280',
          background: '#f3f4f6',
          padding: '4px 8px',
          borderRadius: '4px',
          fontWeight: '500'
        }}>
          Max: {MAX_FILE_SIZE_MB} MB
        </span>
      </div>

      {/* Preview */}
      {preview && (
        <div style={{
          marginBottom: '16px',
          textAlign: 'center',
          padding: '16px',
          background: '#f9fafb',
          borderRadius: '8px',
          border: '1px solid #e5e7eb'
        }}>
          <img
            src={preview}
            alt="Logo preview"
            style={{
              maxWidth: '200px',
              maxHeight: '200px',
              borderRadius: '8px',
              boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
            }}
            onError={(e) => {
              (e.target as HTMLImageElement).style.display = 'none';
            }}
          />
          {selectedFile && (
            <div style={{
              marginTop: '12px',
              fontSize: '12px',
              color: '#6b7280'
            }}>
              {selectedFile.name} • {formatFileSize(selectedFile.size)}
            </div>
          )}
        </div>
      )}

      {/* Upload Area */}
      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        style={{
          border: isDragging ? '2px dashed #4f46e5' : '2px dashed #d1d5db',
          borderRadius: '8px',
          padding: '32px',
          textAlign: 'center',
          cursor: uploading ? 'not-allowed' : 'pointer',
          background: isDragging ? '#eff6ff' : '#f9fafb',
          transition: 'all 0.2s',
          opacity: uploading ? 0.6 : 1
        }}
      >
        {uploading ? (
          <>
            <div style={{ fontSize: '32px', marginBottom: '8px' }}>⏳</div>
            <p style={{ margin: 0, color: '#6b7280', fontSize: '14px' }}>
              Uploading...
            </p>
          </>
        ) : (
          <>
            <div style={{ fontSize: '32px', marginBottom: '8px' }}>📸</div>
            <p style={{ margin: '0 0 4px 0', color: '#1f2937', fontWeight: '500' }}>
              {preview ? 'Click or drag to change logo' : 'Click or drag to upload logo'}
            </p>
            <p style={{ margin: 0, color: '#6b7280', fontSize: '12px' }}>
              PNG, JPG, or WebP • Max {MAX_FILE_SIZE_MB} MB
            </p>
          </>
        )}
      </div>

      {/* Hidden file input */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/jpg,image/png,image/webp"
        onChange={handleFileInputChange}
        style={{ display: 'none' }}
        disabled={uploading}
      />

      <div style={{
        marginTop: '8px',
        padding: '8px 12px',
        background: '#eff6ff',
        borderRadius: '6px',
        border: '1px solid #bfdbfe'
      }}>
        <p style={{ margin: 0, fontSize: '12px', color: '#1e40af' }}>
          💡 <strong>Tips:</strong> Recommended 200x200px • Keep under {MAX_FILE_SIZE_MB} MB for best performance
        </p>
      </div>
    </div>
  );
}
