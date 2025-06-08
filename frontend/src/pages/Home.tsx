

const Home = () => (
    <div className="flex min-h-screen">
     
      {/* Main area flexes to fill remaining space */}
      <div className="flex-1 bg-background text-foreground transition-colors">
        <main className="container mx-auto px-4 py-10">
          {/* …your dashboard content… */}
        </main>
      </div>
    </div>
  );

  export default Home;
  