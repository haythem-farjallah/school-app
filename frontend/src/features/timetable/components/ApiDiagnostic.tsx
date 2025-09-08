import { useState, useEffect, useCallback } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { 
  CheckCircle, 
  XCircle, 
  Clock, 
  RefreshCw,
  AlertTriangle,
  Server,
  Database,
  Users,
  BookOpen,
  MapPin
} from 'lucide-react';
import { http } from '../../../lib/http';

interface ApiDiagnosticProps {
  classId: number;
}

interface EndpointTest {
  name: string;
  url: string;
  status: 'pending' | 'success' | 'error';
  response?: unknown;
  error?: string;
  count?: number;
}

export function ApiDiagnostic({ classId }: ApiDiagnosticProps) {
  const [tests, setTests] = useState<EndpointTest[]>([
    { name: 'Periods', url: '/v1/periods', status: 'pending' },
    { name: 'Teachers', url: '/admin/teachers?size=100', status: 'pending' },
    { name: 'Courses', url: '/v1/courses?size=100', status: 'pending' },
    { name: 'Rooms', url: '/v1/rooms?size=100', status: 'pending' },
    { name: 'Timetable', url: `/v1/timetables/class/${classId}`, status: 'pending' },
    { name: 'Classes', url: '/v1/classes?size=100', status: 'pending' },
  ]);
  const [isRunning, setIsRunning] = useState(false);

  const testEndpoint = async (test: EndpointTest): Promise<EndpointTest> => {
    try {
      console.log(`Testing endpoint: ${test.url}`);
      const response = await http.get(test.url);
      console.log(`Response from ${test.url}:`, response);
      
      let data = response?.data || response;
      let count = 0;
      
      // Try to extract array data and count
      if (Array.isArray(data)) {
        count = data.length;
      } else if (data?.content && Array.isArray(data.content)) {
        count = data.content.length;
        data = data.content;
      } else if (data?.slots && Array.isArray(data.slots)) {
        count = data.slots.length;
      } else if (data && typeof data === 'object') {
        count = 1; // Single object
      }
      
      return {
        ...test,
        status: 'success',
        response: data,
        count
      };
    } catch (error: unknown) {
      console.error(`Error testing ${test.url}:`, error);
      return {
        ...test,
        status: 'error',
        error: (error as any)?.response?.data?.message || (error as any)?.message || 'Unknown error',
        response: (error as any)?.response?.data
      };
    }
  };

  const runAllTests = useCallback(async () => {
    setIsRunning(true);
    
    for (let i = 0; i < tests.length; i++) {
      const test = tests[i];
      
      // Update status to show current test
      setTests(prev => prev.map((t, idx) => 
        idx === i ? { ...t, status: 'pending' } : t
      ));
      
      const result = await testEndpoint(test);
      
      // Update with result
      setTests(prev => prev.map((t, idx) => 
        idx === i ? result : t
      ));
      
      // Small delay between tests
      await new Promise(resolve => setTimeout(resolve, 500));
    }
    
    setIsRunning(false);
  }, [tests]);

  useEffect(() => {
    runAllTests();
  }, [classId, runAllTests]);

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'success':
        return <CheckCircle className="w-5 h-5 text-green-600" />;
      case 'error':
        return <XCircle className="w-5 h-5 text-red-600" />;
      case 'pending':
        return <Clock className="w-5 h-5 text-yellow-600" />;
      default:
        return <AlertTriangle className="w-5 h-5 text-gray-400" />;
    }
  };

  const getEndpointIcon = (name: string) => {
    switch (name.toLowerCase()) {
      case 'periods':
        return <Clock className="w-4 h-4" />;
      case 'teachers (admin)':
      case 'teachers (v1)':
        return <Users className="w-4 h-4" />;
      case 'courses':
        return <BookOpen className="w-4 h-4" />;
      case 'rooms':
        return <MapPin className="w-4 h-4" />;
      case 'timetable':
        return <Database className="w-4 h-4" />;
      case 'classes':
        return <Server className="w-4 h-4" />;
      default:
        return <Server className="w-4 h-4" />;
    }
  };

  const successCount = tests.filter(t => t.status === 'success').length;
  const errorCount = tests.filter(t => t.status === 'error').length;
  const pendingCount = tests.filter(t => t.status === 'pending').length;

  return (
    <div className="space-y-6">
      {/* Summary */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Server className="w-5 h-5" />
            API Endpoint Diagnostic
            {isRunning && <RefreshCw className="w-4 h-4 animate-spin" />}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-3 gap-4 mb-4">
            <div className="text-center">
              <div className="text-2xl font-bold text-green-600">{successCount}</div>
              <div className="text-sm text-gray-600">Working</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-red-600">{errorCount}</div>
              <div className="text-sm text-gray-600">Failed</div>
            </div>
            <div className="text-center">
              <div className="text-2xl font-bold text-yellow-600">{pendingCount}</div>
              <div className="text-sm text-gray-600">Testing</div>
            </div>
          </div>
          
          <Button 
            onClick={runAllTests} 
            disabled={isRunning}
            className="w-full"
          >
            {isRunning ? (
              <>
                <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                Testing Endpoints...
              </>
            ) : (
              <>
                <RefreshCw className="w-4 h-4 mr-2" />
                Retest All Endpoints
              </>
            )}
          </Button>
        </CardContent>
      </Card>

      {/* Detailed Results */}
      <Card>
        <CardHeader>
          <CardTitle>Endpoint Test Results</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {tests.map((test, index) => (
              <div key={index} className="border rounded-lg p-4">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    {getEndpointIcon(test.name)}
                    <span className="font-medium">{test.name}</span>
                    <Badge variant="outline" className="text-xs">
                      {test.url}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-2">
                    {test.count !== undefined && (
                      <Badge variant="secondary">
                        {test.count} items
                      </Badge>
                    )}
                    {getStatusIcon(test.status)}
                  </div>
                </div>
                
                {test.status === 'error' && (
                  <div className="mt-2 p-2 bg-red-50 border border-red-200 rounded text-sm">
                    <div className="font-medium text-red-800">Error:</div>
                    <div className="text-red-700">{test.error}</div>
                    {test.response && (
                      <details className="mt-2">
                        <summary className="cursor-pointer text-red-600">Response Details</summary>
                        <pre className="mt-1 text-xs overflow-auto">
                          {JSON.stringify(test.response, null, 2)}
                        </pre>
                      </details>
                    )}
                  </div>
                )}
                
                {test.status === 'success' && test.response && (
                  <details className="mt-2">
                    <summary className="cursor-pointer text-green-600 text-sm">
                      View Response ({test.count} items)
                    </summary>
                    <pre className="mt-1 text-xs bg-green-50 p-2 rounded overflow-auto max-h-40">
                      {JSON.stringify(test.response, null, 2)}
                    </pre>
                  </details>
                )}
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Recommendations */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <AlertTriangle className="w-5 h-5 text-yellow-600" />
            Recommendations
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-2 text-sm">
            {errorCount > 0 && (
              <div className="p-3 bg-red-50 border border-red-200 rounded">
                <div className="font-medium text-red-800">❌ Backend Issues Detected</div>
                <div className="text-red-700">
                  {errorCount} endpoint(s) are failing. Check if:
                  <ul className="list-disc list-inside mt-1 ml-4">
                    <li>Backend server is running (mvn spring-boot:run)</li>
                    <li>Database is connected and has data</li>
                    <li>No migration conflicts</li>
                  </ul>
                </div>
              </div>
            )}
            
            {tests.find(t => t.name.includes('Teachers') && t.status === 'error') && (
              <div className="p-3 bg-yellow-50 border border-yellow-200 rounded">
                <div className="font-medium text-yellow-800">⚠️ No Teachers Found</div>
                <div className="text-yellow-700">
                  Create some teachers first using the admin panel or data initializer.
                </div>
              </div>
            )}
            
            {tests.find(t => t.name === 'Courses' && t.status === 'error') && (
              <div className="p-3 bg-yellow-50 border border-yellow-200 rounded">
                <div className="font-medium text-yellow-800">⚠️ No Courses Found</div>
                <div className="text-yellow-700">
                  Create some courses first using the admin panel.
                </div>
              </div>
            )}
            
            {tests.find(t => t.name === 'Rooms' && t.status === 'error') && (
              <div className="p-3 bg-yellow-50 border border-yellow-200 rounded">
                <div className="font-medium text-yellow-800">⚠️ No Rooms Found</div>
                <div className="text-yellow-700">
                  Create some rooms first using the admin panel.
                </div>
              </div>
            )}
            
            {successCount === tests.length && (
              <div className="p-3 bg-green-50 border border-green-200 rounded">
                <div className="font-medium text-green-800">✅ All Endpoints Working</div>
                <div className="text-green-700">
                  All API endpoints are responding correctly. The timetable should work properly.
                </div>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
